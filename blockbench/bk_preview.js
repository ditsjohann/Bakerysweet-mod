(function () {
// Renders a contact sheet of saved textures (prefix filter) to the scratchpad for review.
const ART = window.ART, fs = ART.fs, path = ART.path;
const opt = window.ART_PREVIEW || {};
const files = (opt.files || ART.saved).filter(f => !opt.prefix || f.startsWith(opt.prefix));
const scale = opt.scale || 5, pad = 6, cols = opt.cols || 10;
const cellW = 16 * scale + pad, cellH = 16 * scale + pad + 10;
const cv = document.createElement('canvas');
cv.width = cols * cellW; cv.height = Math.ceil(files.length / cols) * cellH;
const ctx = cv.getContext('2d');
ctx.imageSmoothingEnabled = false;
ctx.fillStyle = opt.bg || '#8b9bb0'; ctx.fillRect(0, 0, cv.width, cv.height);
ctx.font = '9px sans-serif'; ctx.fillStyle = '#ffffff';
return Promise.all(files.map((f, i) => new Promise(res => {
	const img = new Image();
	img.onload = () => {
		const x = (i % cols) * cellW + pad / 2, y = Math.floor(i / cols) * cellH + pad / 2;
		const s = Math.min(16 * scale / img.width, 16 * scale / img.height);
		ctx.drawImage(img, x, y, img.width * s, img.height * s);
		ctx.fillText(f.split('/').pop().slice(0, 16), x, y + 16 * scale + 9);
		res();
	};
	img.src = 'data:image/png;base64,' + fs.readFileSync(path.join(ART.ROOT, 'textures', f + '.png')).toString('base64');
}))).then(() => {
	fs.writeFileSync(opt.out, Buffer.from(cv.toDataURL('image/png').split(',')[1], 'base64'));
	return files.length + ' previewed';
});
})()
