module.exports = {
    env: {
        es2021: true,
        node: true,
    },
    extends: [
        'eslint:recommended',
        'google',
    ],
    parserOptions: {
        ecmaVersion: 12,
        sourceType: 'module',
    },
    rules: {
        'indent': ['error', 4, { 'SwitchCase': 1 }],
        'max-len': ['error', {
            code: 125,
            ignoreUrls: true,
            ignoreStrings: true,
            ignoreTemplateLiterals: true,
            ignoreRegExpLiterals: true,
        }],

        'eol-last': ['error', 'always'],
        'quotes': ['error', 'single'],
        'no-trailing-spaces': 'error',
        'comma-dangle': ['error', 'always-multiline'],
        'require-jsdoc': 'off',
        'object-curly-spacing': ['error', 'always'],
        'brace-style': ['error', '1tbs', { 'allowSingleLine': true }],
    },
};
