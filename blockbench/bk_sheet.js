(function () {
// Contact sheet of textures (paths relative to textures/, e.g. 'item/sweet_coin'), written to build/sheet.png.
// Usage in the art page: window.SHEET_LIST = [...]; run('.../bk_sheet.js') then await window.SHEET_DONE.
const fs = require('fs');
const root = 'C:/Users/ditsj/OneDrive/Bureaublad/Bakery mod/src/main/resources/assets/bakery/textures/';
const list = window.SHEET_LIST || window.ART.saved.slice();
const S = 5, CW = 16 * S + 8, cols = 10;
const c = document.createElement('canvas');
c.width = cols * CW;
c.height = Math.ceil(list.length / cols) * (CW + 10);
const g = c.getContext('2d');
g.imageSmoothingEnabled = false;
g.fillStyle = '#9cc8f0';
g.fillRect(0, 0, c.width, c.height);
g.font = '9px sans-serif';
const load = p => new Promise(r => {
	const m = new Image();
	m.onload = () => r(m);
	m.onerror = () => r(null);
	m.src = '/raw?path=' + encodeURIComponent(root + p + '.png');
});
window.SHEET_DONE = (async () => {
	for (let i = 0; i < list.length; i++) {
		const x = (i % cols) * CW, y = Math.floor(i / cols) * (CW + 10);
		const im = await load(list[i]);
		if (im) {
			const w = 16 * S * Math.min(1, im.width / im.height * 1), h = w * im.height / im.width;
			g.drawImage(im, x + 4, y + 4, im.width > im.height ? 16 * S : w, im.width > im.height ? 16 * S * im.height / im.width : h);
		}
		g.fillStyle = '#222';
		g.fillText(list[i].replace(/^.*\//, '').slice(0, 17), x + 2, y + CW + 6);
	}
	fs.writeFileSync('C:/Users/ditsj/OneDrive/Bureaublad/Bakery mod/build/sheet.png', Buffer.from(c.toDataURL('image/png').split(',')[1], 'base64'));
	return list.length;
})();
return 'started';
})()
