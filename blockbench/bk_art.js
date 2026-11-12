(function () {
// Sweetheart Bakery pixel-art engine (v2): volumetric shading, hue-shifted ramps, selective outlines.
const fs = require('fs');
const path = require('path');
const ROOT = 'C:/Users/ditsj/OneDrive/Bureaublad/Bakery mod/src/main/resources/assets/bakery/';

// ---------- color ----------
function parse(c) {
	c = c.replace('!', '');
	const a = c.length === 9 ? parseInt(c.slice(7, 9), 16) : 255;
	return { r: parseInt(c.slice(1, 3), 16), g: parseInt(c.slice(3, 5), 16), b: parseInt(c.slice(5, 7), 16), a };
}
function toHex(c) { return '#' + [c.r, c.g, c.b].map(v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0')).join(''); }
function rgb2hsl({ r, g, b }) {
	r /= 255; g /= 255; b /= 255;
	const max = Math.max(r, g, b), min = Math.min(r, g, b), l = (max + min) / 2;
	let h = 0, s = 0;
	if (max !== min) {
		const d = max - min;
		s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
		h = max === r ? (g - b) / d + (g < b ? 6 : 0) : max === g ? (b - r) / d + 2 : (r - g) / d + 4;
		h *= 60;
	}
	return { h, s, l };
}
function hsl2rgb({ h, s, l }) {
	h = ((h % 360) + 360) % 360 / 360;
	if (s === 0) return { r: l * 255, g: l * 255, b: l * 255 };
	const q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
	const f = t => { t = (t + 1) % 1; return t < 1 / 6 ? p + (q - p) * 6 * t : t < 1 / 2 ? q : t < 2 / 3 ? p + (q - p) * (2 / 3 - t) * 6 : p; };
	return { r: f(h + 1 / 3) * 255, g: f(h) * 255, b: f(h - 1 / 3) * 255 };
}
function towardHue(h, target, amt) {
	let d = ((target - h + 540) % 360) - 180;
	return h + Math.sign(d) * Math.min(Math.abs(d), amt);
}
const STEPS = [-0.32, -0.2, -0.1, 0, 0.085, 0.165];
// Six-step ramp around a base color (index 3). Shadows drift to purple, highlights to warm yellow.
function ramp(base, neutral) {
	const c = parse(base), hsl = rgb2hsl(c);
	return STEPS.map(t => {
		let { h, s, l } = hsl;
		const room = t < 0 ? l : 1 - l;
		l = Math.max(0.04, Math.min(0.97, l + t * (0.6 + room * 0.6)));
		if (!neutral && s > 0.05) {
			if (t < 0) { h = towardHue(h, 285, -t * 55); s = Math.min(1, s + -t * 0.25); }
			else if (t > 0) { h = towardHue(h, 55, t * 70); s = Math.max(0, s - t * 0.5); }
		}
		const o = hsl2rgb({ h, s, l });
		return { r: o.r, g: o.g, b: o.b, a: c.a };
	});
}
function mix(a, b, t) { return { r: a.r + (b.r - a.r) * t, g: a.g + (b.g - a.g) * t, b: a.b + (b.b - a.b) * t, a: a.a }; }
const INK = parse('#2b1424');
function rnd(x, y, s) { let n = (x * 374761393 + y * 668265263 + (s || 0) * 982451653) | 0; n = (n ^ (n >>> 13)) * 1274126177 | 0; return ((n ^ (n >>> 16)) >>> 0) / 4294967295; }

// ---------- masks ----------
const M = {
	el: (cx, cy, rx, ry, rot, ring) => {
		const a = (rot || 0) * Math.PI / 180, cs = Math.cos(a), sn = Math.sin(a);
		return (x, y) => { const dx = x - cx, dy = y - cy, u = (dx * cs + dy * sn) / rx, v = (-dx * sn + dy * cs) / ry, d = u * u + v * v; return ring ? d <= 1 && d > ring * ring : d <= 1; };
	},
	rc: (x0, y0, x1, y1) => (x, y) => x >= x0 && x <= x1 + 1 && y >= y0 && y <= y1 + 1,
	pg: pts => (px, py) => {
		let ins = false;
		for (let i = 0, j = pts.length - 1; i < pts.length; j = i++) {
			const [xi, yi] = pts[i], [xj, yj] = pts[j];
			if ((yi > py) !== (yj > py) && px < (xj - xi) * (py - yi) / (yj - yi) + xi) ins = !ins;
		}
		return ins;
	},
	ln: (x0, y0, x1, y1, w) => (px, py) => {
		const dx = x1 - x0, dy = y1 - y0, l = dx * dx + dy * dy;
		let t = l ? ((px - x0) * dx + (py - y0) * dy) / l : 0; t = Math.max(0, Math.min(1, t));
		const qx = x0 + t * dx - px, qy = y0 + t * dy - py;
		return qx * qx + qy * qy <= (w / 2) * (w / 2);
	},
	star: (cx, cy, r1, r2, rot) => {
		const pts = []; const a0 = ((rot == null ? -90 : rot)) * Math.PI / 180;
		for (let i = 0; i < 10; i++) { const r = i % 2 ? r2 : r1, a = a0 + i * Math.PI / 5; pts.push([cx + Math.cos(a) * r, cy + Math.sin(a) * r]); }
		return M.pg(pts);
	},
	heart: (cx, cy, s) => (x, y) => {
		const u = (x - cx) / s, v = -(y - cy) / s + 0.25;
		const q = u * u + v * v - 1;
		return q * q * q - u * u * v * v * v <= 0;
	},
	pts: list => { const set = new Set(list.map(([x, y]) => x + ',' + y)); return (x, y) => set.has(Math.floor(x) + ',' + Math.floor(y)); },
	or: (...ms) => (x, y) => ms.some(m => m(x, y)),
	and: (a, b) => (x, y) => a(x, y) && b(x, y),
	not: m => (x, y) => !m(x, y)
};

const LIGHT = (() => { const v = [-0.62, -0.78, 1.0], l = Math.hypot(...v); return v.map(c => c / l); })();

class Art {
	constructor(w, h) {
		this.w = w || 16; this.h = h || 16;
		this.id = new Int32Array(this.w * this.h);
		this.shapes = [null];
		this.decals = [];
		this.fixed = [];
	}
	add(mask, base, opts) {
		opts = opts || {};
		const sid = this.shapes.length;
		this.shapes.push({ ramp: Array.isArray(base) ? base : ramp(base, opts.neutral), opts });
		for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) if (mask(x + 0.5, y + 0.5)) this.id[y * this.w + x] = sid;
		return this;
	}
	el(cx, cy, rx, ry, base, opts) { return this.add(M.el(cx, cy, rx, ry, opts && opts.rot, opts && opts.ring), base, opts); }
	rc(x0, y0, x1, y1, base, opts) { return this.add(M.rc(x0, y0, x1, y1), base, opts); }
	pg(pts, base, opts) { return this.add(M.pg(pts), base, opts); }
	ln(x0, y0, x1, y1, w, base, opts) { return this.add(M.ln(x0, y0, x1, y1, w), base, opts); }
	star(cx, cy, r1, r2, base, opts) { return this.add(M.star(cx, cy, r1, r2, opts && opts.rot), base, opts); }
	heart(cx, cy, s, base, opts) { return this.add(M.heart(cx, cy, s), base, opts); }
	cut(mask) { for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) if (mask(x + 0.5, y + 0.5)) this.id[y * this.w + x] = 0; return this; }
	// Decal pixels: base ramp, level relative to what is underneath (shift) or fixed (lvl).
	dec(mask, base, opts) { this.decals.push({ mask, ramp: ramp(base, opts && opts.neutral), opts: opts || {} }); return this; }
	dots(list, base, opts) { return this.dec(M.pts(list), base, opts); }
	px(x, y, hex) { this.fixed.push([x, y, parse(hex)]); return this; }
	pxs(list, hex) { list.forEach(([x, y]) => this.px(x, y, hex)); return this; }

	render() {
		const W = this.w, H = this.h, N = W * H;
		const lvl = new Int8Array(N).fill(-1);
		const diffs = new Float32Array(N);
		for (let sid = 1; sid < this.shapes.length; sid++) {
			const { opts } = this.shapes[sid];
			const inS = (x, y) => x >= 0 && y >= 0 && x < W && y < H && this.id[y * W + x] === sid;
			// distance to the shape's edge (8-neighbour BFS)
			const dist = new Float32Array(N);
			let queue = [];
			let any = false;
			for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
				if (!inS(x, y)) continue;
				any = true;
				let edge = false;
				for (let dy = -1; dy <= 1 && !edge; dy++) for (let dx = -1; dx <= 1; dx++) if ((dx || dy) && !inS(x + dx, y + dy)) { edge = true; break; }
				if (edge) { dist[y * W + x] = 1; queue.push([x, y]); }
			}
			if (!any) continue;
			let maxD = 1;
			while (queue.length) {
				const next = [];
				for (const [x, y] of queue) {
					for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) {
						const nx = x + dx, ny = y + dy;
						if (inS(nx, ny) && !dist[ny * W + nx]) { dist[ny * W + nx] = dist[y * W + x] + 1; maxD = Math.max(maxD, dist[ny * W + nx]); next.push([nx, ny]); }
					}
				}
				queue = next;
			}
			const R = Math.max(1, Math.min(maxD, opts.radius || maxD));
			const bulge = opts.bulge == null ? 1 : opts.bulge;
			const prof = opts.profile || 'dome';
			const hgt = (x, y) => {
				if (!inS(x, y)) return 0;
				const t = Math.min(1, dist[y * W + x] / R);
				if (prof === 'flat') return t >= 1 ? R : dist[y * W + x];
				if (prof === 'soft') return Math.sin(t * Math.PI / 2) * R * bulge * 0.7;
				return Math.sqrt(1 - (1 - t) * (1 - t)) * R * bulge;
			};
			const flat0 = LIGHT[2];
			for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
				if (!inS(x, y)) continue;
				let l;
				if (prof === 'flat') {
					const up = !inS(x, y - 1) || !inS(x - 1, y), dn = !inS(x, y + 1) || !inS(x + 1, y);
					l = (opts.flatLvl == null ? 3 : opts.flatLvl) + (up ? 1 : dn ? -1 : 0);
					diffs[y * W + x] = up ? 1 : 0;
				} else {
					const dx = hgt(x + 1, y) - hgt(x - 1, y), dy = hgt(x, y + 1) - hgt(x, y - 1);
					const n = [-dx, -dy, 2], nl = Math.hypot(...n);
					const diff = (n[0] * LIGHT[0] + n[1] * LIGHT[1] + n[2] * LIGHT[2]) / nl;
					diffs[y * W + x] = diff;
					l = Math.round(3 + (diff - flat0) * (opts.contrast || 7));
				}
				if (opts.grain && rnd(x, y, sid) < opts.grain) l += rnd(x, y, sid + 9) < 0.5 ? -1 : 1;
				// cast shadow from a shape drawn above (later) on the top/left side
				const above = y > 0 ? this.id[(y - 1) * W + x] : 0, left = x > 0 ? this.id[y * W + x - 1] : 0;
				if ((above > sid || left > sid) && !opts.noShadow) l -= 1;
				lvl[y * W + x] = Math.max(0, Math.min(5, l));
			}
		}
		const out = new Array(N).fill(null);
		for (let i = 0; i < N; i++) {
			const sid = this.id[i]; if (!sid) continue;
			out[i] = Object.assign({}, this.shapes[sid].ramp[lvl[i]]);
		}
		// shine: brightest pixel of glossy shapes goes almost white
		for (let sid = 1; sid < this.shapes.length; sid++) {
			const { opts, ramp: rp } = this.shapes[sid];
			if (!opts.shine) continue;
			let best = -1, bd = -9;
			for (let i = 0; i < N; i++) if (this.id[i] === sid && diffs[i] > bd) { bd = diffs[i]; best = i; }
			if (best >= 0) {
				out[best] = mix(rp[5], { r: 255, g: 255, b: 255 }, 0.65);
				if (opts.shine > 1 && best + 1 < N && this.id[best + 1] === sid) out[best + 1] = mix(rp[5], { r: 255, g: 255, b: 255 }, 0.35);
			}
		}
		// decals
		for (const d of this.decals) {
			for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
				if (!d.mask(x + 0.5, y + 0.5)) continue;
				const i = y * W + x;
				let l = d.opts.lvl != null ? d.opts.lvl : (lvl[i] >= 0 ? lvl[i] : 3) + (d.opts.shift || 0);
				l = Math.max(0, Math.min(5, l));
				out[i] = Object.assign({}, d.ramp[l]);
				if (!this.id[i]) { this.id[i] = -1; }
			}
		}
		// selective outline
		const res = out.slice();
		for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
			const i = y * W + x;
			if (this.id[i]) continue;
			const nb = [[0, 1, 'top'], [0, -1, 'bottom'], [1, 0, 'left'], [-1, 0, 'right']];
			let pick = null, side = null;
			for (const [dx, dy, s] of nb) {
				const nx = x + dx, ny = y + dy;
				if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
				const sid = this.id[ny * W + nx];
				if (sid > 0 && this.shapes[sid].opts.outline !== false && (!pick || sid > pick)) { pick = sid; side = s; }
			}
			if (!pick) continue;
			const rp = this.shapes[pick].ramp;
			const o = (side === 'top' || side === 'left') ? mix(rp[1], INK, 0.35) : mix(rp[0], INK, 0.5);
			res[i] = { r: o.r, g: o.g, b: o.b, a: 255 };
		}
		for (const [x, y, c] of this.fixed) res[y * W + x] = c;
		this.out = res;
		return this;
	}
	canvas(scale) {
		if (!this.out) this.render();
		scale = scale || 1;
		const cv = document.createElement('canvas'); cv.width = this.w * scale; cv.height = this.h * scale;
		const ctx = cv.getContext('2d');
		this.out.forEach((c, i) => {
			if (!c || c.a === 0) return;
			ctx.fillStyle = `rgba(${Math.round(c.r)},${Math.round(c.g)},${Math.round(c.b)},${c.a / 255})`;
			ctx.fillRect((i % this.w) * scale, Math.floor(i / this.w) * scale, scale, scale);
		});
		return cv;
	}
	save(rel) {
		const file = path.join(ROOT, 'textures', rel + '.png');
		fs.mkdirSync(path.dirname(file), { recursive: true });
		fs.writeFileSync(file, Buffer.from(this.canvas().toDataURL('image/png').split(',')[1], 'base64'));
		window.ART.saved.push(rel);
		return this;
	}
}

// Raw pixel texture for tiling block faces: fn(x, y) -> '#hex' | null
class Tex {
	constructor(w, h) { this.w = w || 16; this.h = h || 16; this.p = new Array(this.w * this.h).fill(null); }
	fill(fn) { for (let y = 0; y < this.h; y++) for (let x = 0; x < this.w; x++) { const c = fn(x, y); if (c !== undefined) this.p[y * this.w + x] = c ? parse(c) : null; } return this; }
	set(x, y, c) { x = ((x % this.w) + this.w) % this.w; y = ((y % this.h) + this.h) % this.h; this.p[y * this.w + x] = c ? parse(c) : null; return this; }
	get(x, y) { x = ((x % this.w) + this.w) % this.w; y = ((y % this.h) + this.h) % this.h; return this.p[y * this.w + x]; }
	save(rel) {
		const cv = document.createElement('canvas'); cv.width = this.w; cv.height = this.h;
		const ctx = cv.getContext('2d'), img = ctx.createImageData(this.w, this.h);
		this.p.forEach((c, i) => { if (!c) return; img.data[i * 4] = c.r; img.data[i * 4 + 1] = c.g; img.data[i * 4 + 2] = c.b; img.data[i * 4 + 3] = c.a; });
		ctx.putImageData(img, 0, 0);
		const file = path.join(ROOT, 'textures', rel + '.png');
		fs.mkdirSync(path.dirname(file), { recursive: true });
		fs.writeFileSync(file, Buffer.from(cv.toDataURL('image/png').split(',')[1], 'base64'));
		window.ART.saved.push(rel);
		return this;
	}
}

window.ART = { fs, path, ROOT, ramp, parse, toHex, mix, rnd, M, Art, Tex, A: (w, h) => new Art(w, h), T: (w, h) => new Tex(w, h), saved: [],
	shade: (base, i, neutral) => toHex(ramp(base, neutral)[i]) };
return 'ART ready';
})()
