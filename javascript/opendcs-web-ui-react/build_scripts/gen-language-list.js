import {readdirSync, writeFile} from 'node:fs'


const locales = readdirSync('public/locales');

const langs = `const availableLanguages: Array<string> = ${JSON.stringify(locales)};\n` +
              "export default availableLanguages";

writeFile("src/lang/index.ts", langs, err => {
    if (err) {
        console.log(err);
    }
})

