const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { GoogleAuth } = require('google-auth-library');

// Khởi tạo Firebase Admin SDK với file service account
const serviceAccount = require('./shopease-new-12345-f1f4ec8a1b96.json');
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
});

// Hàm gửi thông báo qua HTTP v1 API
async function sendFCMViaHTTP(tokens, payload) {
    const auth = new GoogleAuth({
        credentials: serviceAccount,
        scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
    });

    try {
        // Lấy access token
        const client = await auth.getClient();
        const accessToken = await client.getAccessToken();
        const url = `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`;

        // Gửi yêu cầu cho từng token
        const responses = [];
        for (const token of tokens) {
            const message = {
                message: {
                    token: token,
                    notification: payload.notification,
                    data: payload.data,
                    android: payload.android,
                },
            };

            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken.token}`,
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(message),
            });

            const data = await response.json();
            responses.push({ token, response: data });

            if (!response.ok) {
                console.error(`[sendFCMViaHTTP] Failed to send to token ${token}:`, data);
            } else {
                console.log(`[sendFCMViaHTTP] Successfully sent to token ${token}:`, data);
            }
        }
        return responses;
    } catch (error) {
        console.error('[sendFCMViaHTTP] Error sending notification:', error);
        throw error;
    }
}

// Hàm gửi thông báo khi có tin nhắn chat mới
exports.sendChatNotification = functions.firestore
    .document('chats/{roomId}/messages/{messageId}')
    .onCreate(async (snap, context) => {
        const messageData = snap.data();
        const roomId = context.params.roomId;
        const messageId = context.params.messageId;
        console.log(`[sendChatNotification] New message in room ${roomId}, messageId: ${messageId}, data:`, JSON.stringify(messageData));

        // Kiểm tra snapshot và dữ liệu
        if (!snap.exists || !messageData) {
            console.error(`[sendChatNotification] No data found for messageId: ${messageId}. Aborting.`);
            return null;
        }

        // Kiểm tra các trường bắt buộc
        if (!messageData.message || messageData.admin === undefined) {
            console.error(`[sendChatNotification] Missing required fields for messageId: ${messageId}. message=${messageData.message}, admin=${messageData.admin}`);
            await admin.firestore().collection('notifications').doc('error_logs').collection('items').add({
                error: 'Missing required fields in sendChatNotification',
                messageId: messageId,
                roomId: roomId,
                data: messageData,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
            });
            return null;
        }

        const message = messageData.message;
        const isAdmin = messageData.admin;
        const receiverId = isAdmin ? roomId : '3QRm0nJTKnU9OpVul6N7kEV0OFF3';
        console.log(`[sendChatNotification] ReceiverId: ${receiverId}, isAdmin: ${isAdmin}`);

        try {
            // Lấy token FCM của người nhận
            const userDoc = await admin.firestore().collection('users').doc(receiverId).get();
            if (!userDoc.exists) {
                console.error(`[sendChatNotification] No user document found for receiver: ${receiverId}. Aborting notification.`);
                return null;
            }

            const fcmTokens = userDoc.data().fcmTokens || (userDoc.data().fcmToken ? [userDoc.data().fcmToken] : []);
            console.log(`[sendChatNotification] FCM Tokens for ${receiverId}:`, fcmTokens);

            // Chuẩn bị payload thông báo
            const notificationTitle = isAdmin ? 'Tin nhắn mới từ Admin' : `Tin nhắn mới từ ${userDoc.data().userName || 'Người dùng'}`;
            const payload = {
                notification: {
                    title: notificationTitle,
                    body: message,
                },
                data: {
                    roomId: roomId,
                    type: 'chat',
                    messageId: messageId,
                    click_action: 'FLUTTER_NOTIFICATION_CLICK',
                },
                android: {
                    priority: 'high',
                },
            };

            // Lưu thông báo vào Firestore
            await admin.firestore().collection('notifications').doc(receiverId).collection('items').add({
                title: payload.notification.title,
                body: payload.notification.body,
                roomId: roomId,
                type: 'chat',
                messageId: messageId,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                read: false,
            });
            console.log(`[sendChatNotification] Saved notification to Firestore for receiver: ${receiverId}`);

            if (!fcmTokens || fcmTokens.length === 0) {
                console.warn(`[sendChatNotification] No FCM tokens found for receiver: ${receiverId}. Notification stored but not sent.`);
                return null;
            }

            // Gửi thông báo qua HTTP v1 API
            await sendFCMViaHTTP(fcmTokens, payload);
            return null; // Không cần trả về response vì đã log chi tiết
        } catch (error) {
            console.error(`[sendChatNotification] Error sending chat notification for messageId ${messageId}:`, error);
            await admin.firestore().collection('notifications').doc('error_logs').collection('items').add({
                error: error.message,
                messageId: messageId,
                roomId: roomId,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
            });
            return null;
        }
    });

// Hàm gửi thông báo khi trạng thái đơn hàng thay đổi
exports.sendOrderStatusNotification = functions.firestore
    .document('orders/{orderParentId}/items/{itemId}')
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const oldData = change.before.data();
        const orderParentId = context.params.orderParentId;
        const itemId = context.params.itemId;
        console.log(`[sendOrderStatusNotification] Order update for orderParentId=${orderParentId}, itemId=${itemId}. New data:`, JSON.stringify(newData));

        // Kiểm tra nếu trạng thái thay đổi
        if (newData.status !== oldData.status) {
            const userId = newData.userId;
            const status = newData.status;
            console.log(`[sendOrderStatusNotification] Status changed from '${oldData.status}' to '${newData.status}' for userId: ${userId}`);

            try {
                // Lấy token FCM của người dùng
                const userDoc = await admin.firestore().collection('users').doc(userId).get();
                if (!userDoc.exists) {
                    console.error(`[sendOrderStatusNotification] No user document found for user: ${userId}. Aborting notification.`);
                    return null;
                }

                const fcmTokens = userDoc.data().fcmTokens || (userDoc.data().fcmToken ? [userDoc.data().fcmToken] : []);
                console.log(`[sendOrderStatusNotification] FCM Tokens for userId ${userId}:`, fcmTokens);

                // Chuẩn bị payload thông báo
                const payload = {
                    notification: {
                        title: 'Cập nhật trạng thái đơn hàng',
                        body: `Đơn hàng ${itemId} đã thay đổi trạng thái thành: ${status}`,
                    },
                    data: {
                        orderParentId: orderParentId,
                        itemId: itemId,
                        type: 'order',
                        click_action: 'FLUTTER_NOTIFICATION_CLICK',
                    },
                    android: {
                        priority: 'high',
                    },
                };

                // Lưu thông báo vào Firestore
                await admin.firestore().collection('notifications').doc(userId).collection('items').add({
                    title: payload.notification.title,
                    body: payload.notification.body,
                    orderParentId: orderParentId,
                    itemId: itemId,
                    type: 'order',
                    status: status,
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    read: false,
                });
                console.log(`[sendOrderStatusNotification] Saved notification to Firestore for user: ${userId}`);

                if (!fcmTokens || fcmTokens.length === 0) {
                    console.warn(`[sendOrderStatusNotification] No FCM tokens found for user: ${userId}. Notification stored but not sent.`);
                    return null;
                }

                // Gửi thông báo qua HTTP v1 API
                await sendFCMViaHTTP(fcmTokens, payload);
                return null;
            } catch (error) {
                console.error(`[sendOrderStatusNotification] Error sending order status notification for itemId ${itemId}:`, error);
                return null;
            }
        }
        console.log(`[sendOrderStatusNotification] No status change for orderParentId=${orderParentId}, itemId=${itemId}. Skipping notification.`);
        return null;
    });

