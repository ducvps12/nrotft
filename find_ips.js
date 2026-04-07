const fs = require('fs');
const files = [
    'C:\\Users\\Administrator\\Pictures\\ThoiDaiNgocRong\\Thời Đại Ngọc Rồng_Data\\globalgamemanagers',
    'C:\\Users\\Administrator\\Pictures\\ThoiDaiNgocRong\\Thời Đại Ngọc Rồng_Data\\il2cpp_data\\Metadata\\global-metadata.dat'
];

files.forEach(file => {
    try {
        const buffer = fs.readFileSync(file);
        const str = buffer.toString('ascii');
        let urls = str.match(/https?:\/\/[a-zA-Z0-9.-]+(\/?[a-zA-Z0-9.\-_]*)/g);
        let ips = str.match(/\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b/g);
        
        console.log(`=== ${file} ===`);
        if (urls) {
            urls = [...new Set(urls)];
            console.log(`URLs:`);
            console.log(urls.join('\n'));
        }
        if (ips) {
            ips = [...new Set(ips)];
            console.log(`IPs:`);
            console.log(ips.filter(ip => ip !== '0.0.0.0' && ip !== '255.255.255.255' && !ip.startsWith('10.')).join('\n'));
        }
    } catch (e) {
        console.log("Error: " + e.message);
    }
});
