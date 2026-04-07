const fs = require('fs');
const path = require('path');

const targetDir = 'C:\\Users\\Administrator\\Pictures\\ThoiDaiNgocRong';
const oldStr = Buffer.from('163.61.111.226');
const newStr = Buffer.alloc(14, 0); // nulls
Buffer.from('nrobest.com').copy(newStr);

function walk(dir) {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach(function(file) {
        file = path.join(dir, file);
        const stat = fs.statSync(file);
        if (stat && stat.isDirectory()) {
            results = results.concat(walk(file));
        } else {
            results.push(file);
        }
    });
    return results;
}

const files = walk(targetDir);
for (let file of files) {
    if (!file.endsWith('.dat') && !file.endsWith('.dll') && !file.endsWith('.assets') && !file.includes('globalgamemanagers') && !file.includes('level')) continue;
    
    try {
        let buffer = fs.readFileSync(file);
        let index = buffer.indexOf(oldStr);
        let modified = false;
        
        while(index !== -1) {
            console.log(`Found string in ${file} at offset ${index}`);
            newStr.copy(buffer, index);
            modified = true;
            index = buffer.indexOf(oldStr, index + oldStr.length);
        }
        
        if (modified) {
            console.log(`Saving ${file}`);
            fs.writeFileSync(file, buffer);
        }
    } catch (e) {}
}
console.log('Patch complete!');
