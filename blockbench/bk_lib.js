(function () {
const fs = require('fs');
const path = require('path');
const ROOT = 'C:/Users/ditsj/OneDrive/Bureaublad/Bakery mod/src/main/resources/assets/bakery/';

function hex(c) {
	if (c == null) return null;
	let flat = false;
	if (c.endsWith('!')) { flat = true; c = c.slice(0, -1); }
	let a = 255;
	if (c.length === 9) { a = parseInt(c.slice(7, 9), 16); }
	return { r: parseInt(c.slice(1, 3), 16), g: parseInt(c.slice(3, 5), 16), b: parseInt(c.slice(5, 7), 16), a, flat, key: c };
}
function mix(c, d, t) { return { r: Math.round(c.r + (d.r - c.r) * t), g: Math.round(c.g + (d.g - c.g) * t), b: Math.round(c.b + (d.b - c.b) * t), a: c.a }; }
const WHITE = { r: 255, g: 255, b: 255 }, INK = { r: 52, g: 28, b: 36 };
function light(c, t) { return mix(c, WHITE, t); }
function dark(c, t) { return mix(c, INK, t); }

// Deterministic noise
function rnd(x, y, s) { let n = (x * 374761393 + y * 668265263 + (s || 0) * 982451653) | 0; n = (n ^ (n >>> 13)) * 1274126177 | 0; return ((n ^ (n >>> 16)) >>> 0) / 4294967295; }

class Spr {
	constructor(w, h) { this.w = w || 16; this.h = h || 16; this.p = new Array(this.w * this.h).fill(null); }
	in(x, y) { return x >= 0 && y >= 0 && x < this.w && y < this.h; }
	get(x, y) { return this.in(x, y) ? this.p[y * this.w + x] : null; }
	set(x, y, c) { if (this.in(x, y)) this.p[y * this.w + x] = c === null ? null : (typeof c === 'string' ? hex(c) : c); return this; }
	each(test, c) { for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) if (test(x + 0.5, y + 0.5, x, y)) this.set(x, y, c); return this; }
	el(cx, cy, rx, ry, c, o) {
		o = o || {}; const rot = (o.rot || 0) * Math.PI / 180, cs = Math.cos(rot), sn = Math.sin(rot);
		return this.each((px, py) => {
			const dx = px - cx, dy = py - cy, u = (dx * cs + dy * sn) / rx, v = (-dx * sn + dy * cs) / ry, d = u * u + v * v;
			return o.ring ? d <= 1 && d > o.ring * o.ring : d <= 1;
		}, c);
	}
	rc(x0, y0, x1, y1, c) { return this.each((px, py, x, y) => x >= x0 && x <= x1 && y >= y0 && y <= y1, c); }
	pg(pts, c) {
		return this.each((px, py) => {
			let ins = false;
			for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
				const [xi, yi] = pts[i], [xj, yj] = pts[j];
				if ((yi > py) !== (yj > py) && px < (xj - xi) * (py - yi) / (yj - yi) + xi) ins = !ins;
			}
			return ins;
		}, c);
	}
	ln(x0, y0, x1, y1, w, c) {
		return this.each((px, py) => {
			const dx = x1 - x0, dy = y1 - y0, l = dx * dx + dy * dy;
			let t = l ? ((px - x0) * dx + (py - y0) * dy) / l : 0; t = Math.max(0, Math.min(1, t));
			const qx = x0 + t * dx - px, qy = y0 + t * dy - py;
			return qx * qx + qy * qy <= (w / 2) * (w / 2);
		}, c);
	}
	px(x, y, c) { return this.set(x, y, c); }
	pxs(list, c) { list.forEach(([x, y]) => this.set(x, y, c)); return this; }
	clear(list) { list.forEach(([x, y]) => this.set(x, y, null)); return this; }
	star(cx, cy, r1, r2, c, rot) {
		const pts = []; rot = (rot || -90) * Math.PI / 180;
		for (let i = 0; i < 10; i++) { const r = i % 2 ? r2 : r1, a = rot + i * Math.PI / 5; pts.push([cx + Math.cos(a) * r, cy + Math.sin(a) * r]); }
		return this.pg(pts, c);
	}
	// Per-pixel texture fill: fn(x,y) returns color string or null
	fill(fn) { for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) { const c = fn(x, y); if (c !== undefined) this.set(x, y, c); } return this; }
	// Shading + outline into final RGBA
	done(o) {
		o = Object.assign({ shade: true, outline: true, hi: 0.28, lo: 0.22, ol: 0.6 }, o || {});
		const out = new Array(this.w * this.h).fill(null);
		const key = (x, y) => { const c = this.get(x, y); return c ? c.key : null; };
		for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) {
			const c = this.get(x, y); if (!c) continue;
			let f = c;
			if (o.shade && !c.flat) {
				const k = c.key;
				if (key(x, y - 1) !== k || key(x - 1, y) !== k) f = light(c, o.hi);
				else if (key(x, y + 1) !== k || key(x + 1, y) !== k) f = dark(c, o.lo);
			}
			out[y * this.w + x] = { r: f.r, g: f.g, b: f.b, a: c.a };
		}
		if (o.outline) {
			const res = out.slice();
			for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) {
				if (this.get(x, y)) continue;
				const n = [[0, -1], [0, 1], [-1, 0], [1, 0]].map(([dx, dy]) => this.get(x + dx, y + dy)).find(c => c && c.a > 160);
				if (n) { const d = dark(n, o.ol); res[y * this.w + x] = { r: d.r, g: d.g, b: d.b, a: 255 }; }
			}
			this.out = res;
		} else this.out = out;
		return this;
	}
	canvas() {
		if (!this.out) this.done();
		const cv = document.createElement('canvas'); cv.width = this.w; cv.height = this.h;
		const ctx = cv.getContext('2d'), img = ctx.createImageData(this.w, this.h);
		this.out.forEach((c, i) => { if (!c) return; img.data[i * 4] = c.r; img.data[i * 4 + 1] = c.g; img.data[i * 4 + 2] = c.b; img.data[i * 4 + 3] = c.a; });
		ctx.putImageData(img, 0, 0); return cv;
	}
	save(rel) {
		const file = path.join(ROOT, 'textures', rel + '.png');
		fs.mkdirSync(path.dirname(file), { recursive: true });
		fs.writeFileSync(file, Buffer.from(this.canvas().toDataURL('image/png').split(',')[1], 'base64'));
		window.BK.saved.push(rel);
		return this;
	}
	// Recolor every pixel through a map of base keys -> new colors
	recolor(map) { const s = new Spr(this.w, this.h); this.p.forEach((c, i) => { s.p[i] = c ? (map[c.key] ? hex(map[c.key] + (c.flat ? '!' : '')) : c) : null; }); return s; }
}

// Texture noise helper: base color jittered
function jitter(c, x, y, amt, seed) { const h = hex(c), t = (rnd(x, y, seed) - 0.5) * 2 * amt; return t > 0 ? light(h, t) : dark(h, -t); }
function rgbHex(c) { return '#' + [c.r, c.g, c.b].map(v => v.toString(16).padStart(2, '0')).join(''); }

window.BK = { fs, path, ROOT, hex, mix, light, dark, rnd, Spr, S: (w, h) => new Spr(w, h), jitter, rgbHex, saved: [],
	J: (c, x, y, amt, seed) => rgbHex(jitter(c, x, y, amt, seed)) };
return 'BK ready';
})()
