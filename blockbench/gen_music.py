"""
Original café music for the Café Jukebox, composed here and synthesized from scratch (no samples): four calm,
cute loops in the spirit of cozy shop music.

    morning_pastries     bossa nova: vibraphone melody, nylon-guitar comping, upright bass, shaker and rim
    sugar_and_sprinkles  bouncy and bright: marimba melody, glockenspiel, plucked chords, soft kick and clap
    rainy_window_latte   lo-fi: electric piano, mellow drums, gentle rain and vinyl crackle
    kitten_lullaby       a music-box waltz with a soft pad

Every track is a whole number of bars and its reverb wraps round, so it loops without a seam. Mono (so it fades
with distance in game), written as Ogg Vorbis with ffmpeg.
Run: python3 blockbench/gen_music.py
"""
import math
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/bakery/sounds/music")
NAMES = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def freq(note):
    """'F#4', 'Bb3', 'C5' -> Hz."""
    letter, rest = note[0], note[1:]
    semi = NAMES[letter]
    while rest and rest[0] in "#b":
        semi += 1 if rest[0] == "#" else -1
        rest = rest[1:]
    midi = 12 * (int(rest) + 1) + semi
    return 440.0 * 2 ** ((midi - 69) / 12)


def env(n, attack, release_at, release):
    """Attack ramp, then 1 until release_at (samples), then an exponential release."""
    e = np.ones(n)
    a = max(1, int(attack * SR))
    e[:a] = np.linspace(0, 1, a)
    r0 = int(release_at)
    if r0 < n:
        t = np.arange(n - r0) / SR
        e[r0:] *= np.exp(-t / max(0.01, release))
    return e


# ------------------------------------------------------------------ instruments (all vectorized)

def vibes(f, dur, vel):
    n = int((dur + 1.6) * SR)
    t = np.arange(n) / SR
    y = np.sin(2 * np.pi * f * t) + 0.18 * np.sin(2 * np.pi * 4 * f * t) * np.exp(-t * 6)
    y *= np.exp(-t * 1.1) * (1 + 0.22 * np.sin(2 * np.pi * 5.2 * t))
    return y * env(n, 0.004, dur * SR, 0.35) * vel


def nylon(f, dur, vel):
    n = int((dur + 1.2) * SR)
    t = np.arange(n) / SR
    y = np.zeros(n)
    for k in range(1, 9):
        y += np.sin(2 * np.pi * k * f * (1 + 0.0004 * k * k) * t) * (1 / k ** 1.25) * np.exp(-t * (1.8 + 1.1 * k))
    return y * env(n, 0.003, dur * SR, 0.12) * vel * 0.8


def marimba(f, dur, vel):
    n = int((dur + 0.9) * SR)
    t = np.arange(n) / SR
    y = np.sin(2 * np.pi * f * t) * np.exp(-t * 4.2) + 0.35 * np.sin(2 * np.pi * 3.93 * f * t) * np.exp(-t * 14) \
        + 0.06 * np.sin(2 * np.pi * 9.2 * f * t) * np.exp(-t * 32)
    return y * env(n, 0.002, (dur + 0.4) * SR, 0.2) * vel


def glock(f, dur, vel):
    n = int((dur + 1.4) * SR)
    t = np.arange(n) / SR
    y = np.sin(2 * np.pi * f * t) * np.exp(-t * 2.4) + 0.28 * np.sin(2 * np.pi * 2.76 * f * t) * np.exp(-t * 5) \
        + 0.12 * np.sin(2 * np.pi * 5.4 * f * t) * np.exp(-t * 9)
    return y * env(n, 0.002, n, 1) * vel


def music_box(f, dur, vel):
    n = int((dur + 2.0) * SR)
    t = np.arange(n) / SR
    y = np.sin(2 * np.pi * f * t) * np.exp(-t * 1.7) + 0.35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 3.2) \
        + 0.18 * np.sin(2 * np.pi * 5.4 * f * t) * np.exp(-t * 7) + 0.07 * np.sin(2 * np.pi * 8.9 * f * t) * np.exp(-t * 14)
    return y * env(n, 0.0015, n, 1) * vel


def epiano(f, dur, vel):
    """A soft FM electric piano (tine and bark fade quickly, the body rings)."""
    n = int((dur + 1.0) * SR)
    t = np.arange(n) / SR
    index = 1.6 * np.exp(-t * 5) + 0.25
    y = np.sin(2 * np.pi * f * t + index * np.sin(2 * np.pi * f * t)) * np.exp(-t * 1.3)
    y += 0.05 * np.sin(2 * np.pi * 14 * f * t) * np.exp(-t * 25)
    return y * env(n, 0.003, dur * SR, 0.25) * vel


def pluck(f, dur, vel):
    n = int((dur + 0.5) * SR)
    t = np.arange(n) / SR
    y = np.zeros(n)
    for k in range(1, 6):
        y += np.sin(2 * np.pi * k * f * t) * (1 / k ** 1.5) * np.exp(-t * (6 + 3 * k))
    return y * env(n, 0.002, dur * SR, 0.06) * vel


def bass(f, dur, vel):
    n = int((dur + 0.5) * SR)
    t = np.arange(n) / SR
    y = np.sin(2 * np.pi * f * t) + 0.22 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 3) + 0.06 * np.sin(2 * np.pi * 3 * f * t) * np.exp(-t * 6)
    y *= np.exp(-t * 0.9)
    return y * env(n, 0.008, dur * SR, 0.08) * vel


def pad(f, dur, vel):
    n = int((dur + 1.2) * SR)
    t = np.arange(n) / SR
    y = np.zeros(n)
    for k in range(1, 5):
        for detune in (-0.0025, 0.0025):
            y += np.sin(2 * np.pi * k * f * (1 + detune) * t) / (k * k)
    return y * env(n, 0.5, dur * SR, 0.7) * vel * 0.25


RNG = np.random.default_rng(7)


def noise(n):
    return RNG.standard_normal(n)


def kick(vel):
    n = int(0.4 * SR)
    t = np.arange(n) / SR
    fr = 48 + 70 * np.exp(-t * 28)
    return np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-t * 8) * vel


def clap(vel):
    n = int(0.25 * SR)
    t = np.arange(n) / SR
    x = np.diff(noise(n + 1))
    return x * (np.exp(-t * 30) + 0.5 * np.exp(-np.maximum(0, t - 0.012) * 30) * (t > 0.012)) * vel * 0.25


def rim(vel):
    n = int(0.12 * SR)
    t = np.arange(n) / SR
    return (np.sin(2 * np.pi * 1700 * t) * 0.6 + np.diff(noise(n + 1)) * 0.3) * np.exp(-t * 55) * vel


def shaker(vel):
    n = int(0.09 * SR)
    t = np.arange(n) / SR
    x = np.diff(np.diff(noise(n + 2)))
    return x * np.exp(-t * 45) * (1 - np.exp(-t * 400)) * vel * 0.12


def hat(vel):
    n = int(0.06 * SR)
    t = np.arange(n) / SR
    return np.diff(np.diff(noise(n + 2))) * np.exp(-t * 70) * vel * 0.1


def snare_soft(vel):
    n = int(0.25 * SR)
    t = np.arange(n) / SR
    return (np.diff(noise(n + 1)) * 0.35 * np.exp(-t * 18) + np.sin(2 * np.pi * 190 * t) * 0.3 * np.exp(-t * 25)) * vel


# ------------------------------------------------------------------ mixing helpers

class Track:
    def __init__(self, bpm, beats_per_bar, bars):
        self.spb = 60.0 / bpm
        self.bpb = beats_per_bar
        self.n = int(round(bars * beats_per_bar * self.spb * SR))
        self.buses = {}

    def add(self, bus, sound, beat, jitter=0.004):
        """Adds a sound starting at `beat` (wrapping round the end, so tails continue at the start)."""
        start = int((beat * self.spb + RNG.uniform(-jitter, jitter)) * SR) % self.n
        buf = self.buses.setdefault(bus, np.zeros(self.n))
        end = start + len(sound)
        if end <= self.n:
            buf[start:end] += sound
        else:
            k = self.n - start
            buf[start:] += sound[:k]
            rest = sound[k:]
            while len(rest):
                m = min(len(rest), self.n)
                buf[:m] += rest[:m]
                rest = rest[m:]

    def note(self, bus, inst, beat, beats, name, vel):
        self.add(bus, inst(freq(name), beats * self.spb, vel * RNG.uniform(0.9, 1.05)), beat)


def reverb(x, seconds=1.6, decay=3.2, wet=0.2):
    """Circular convolution with a soft, darkened noise tail: loops seamlessly."""
    n = len(x)
    m = int(seconds * SR)
    t = np.arange(m) / SR
    ir = noise(m) * np.exp(-t * decay)
    ir = lowpass(ir, 5000)
    ir /= np.sqrt(np.sum(ir ** 2))
    h = np.zeros(n)
    h[:m] = ir
    y = np.fft.irfft(np.fft.rfft(x) * np.fft.rfft(h), n)
    return x + wet * y


def lowpass(x, cutoff, order=2):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    spec *= 1 / np.sqrt(1 + (f / cutoff) ** (2 * order))
    return np.fft.irfft(spec, len(x))


def highpass(x, cutoff, order=2):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    with np.errstate(divide="ignore"):
        spec *= 1 / np.sqrt(1 + (cutoff / np.maximum(f, 1e-3)) ** (2 * order))
    return np.fft.irfft(spec, len(x))


def mix(track, gains, wet=0.2, tone=None):
    y = np.zeros(track.n)
    for bus, buf in track.buses.items():
        y += buf * gains.get(bus, 1.0)
    y = highpass(y, 45)
    if tone:
        y = lowpass(y, tone)
    y = reverb(y, wet=wet)
    y = np.tanh(y / np.max(np.abs(y)) * 1.15)
    return y / np.max(np.abs(y)) * 0.86


def write(name, y):
    os.makedirs(OUT, exist_ok=True)
    pcm = (np.clip(y, -1, 1) * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + ".wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        ogg = os.path.join(OUT, name + ".ogg")
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", "-ac", "1",
                        "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", ogg], check=True)
    return len(y) / SR


def melody(track, bus, inst, bar0, bars, vel, transpose=None):
    for i, notes in enumerate(bars):
        for (beat, beats, name) in notes:
            if transpose:
                name = transpose(name)
            track.note(bus, inst, (bar0 + i) * track.bpb + beat, beats, name, vel)


def octave_up(name):
    return name[:-1] + str(int(name[-1]) + 1)


# ------------------------------------------------------------------ 1. Morning Pastries (bossa nova, F major)

BOSSA_CHORDS = {
    "Fmaj7": ("F2", "C3", ["A3", "C4", "E4", "F4"]), "Gm7": ("G2", "D3", ["Bb3", "D4", "F4", "G4"]),
    "C7": ("C3", "G2", ["Bb3", "E4", "G4"]), "Am7": ("A2", "E3", ["G3", "C4", "E4"]), "D7": ("D3", "A2", ["C4", "F#4", "A4"]),
    "Bbmaj7": ("Bb2", "F3", ["A3", "D4", "F4"]), "Bbm6": ("Bb2", "F3", ["G3", "Db4", "F4"]),
}
BOSSA_A = ["Fmaj7", "Fmaj7", "Gm7", "C7", "Am7", "D7", "Gm7", "C7"]
BOSSA_B = ["Bbmaj7", "Bbm6", "Am7", "D7", "Gm7", "C7", "Fmaj7", "C7"]
BOSSA_MEL_A = [
    [(0, 1.5, "A4"), (1.5, 0.5, "C5"), (2, 1.5, "E5"), (3.5, 0.5, "D5")],
    [(0, 2, "C5"), (2.5, 1.5, "A4")],
    [(0, 1, "Bb4"), (1, 0.5, "D5"), (1.5, 1.5, "F5"), (3, 0.5, "E5"), (3.5, 0.5, "D5")],
    [(0, 2, "E5"), (2.5, 1, "C5"), (3.5, 0.5, "Bb4")],
    [(0, 1.5, "C5"), (1.5, 0.5, "E5"), (2, 1.5, "G5"), (3.5, 0.5, "F5")],
    [(0, 1, "F#5"), (1, 1, "E5"), (2, 1, "D5"), (3, 1, "C5")],
    [(0, 1.5, "Bb4"), (1.5, 0.5, "D5"), (2, 1, "F5"), (3, 1, "E5")],
    [(0, 1, "E5"), (1, 0.5, "D5"), (1.5, 2.5, "C5")],
]
BOSSA_MEL_B = [
    [(0, 1.5, "D5"), (1.5, 0.5, "F5"), (2, 2, "A5")],
    [(0, 1.5, "G5"), (1.5, 0.5, "F5"), (2, 2, "Db5")],
    [(0, 1, "C5"), (1, 1, "E5"), (2, 1.5, "G5"), (3.5, 0.5, "E5")],
    [(0, 2, "F#5"), (2, 1, "A5"), (3, 1, "F#5")],
    [(0, 1.5, "F5"), (1.5, 0.5, "D5"), (2, 2, "Bb4")],
    [(0, 1, "G4"), (1, 1, "Bb4"), (2, 1.5, "E5"), (3.5, 0.5, "G5")],
    [(0, 2, "A5"), (2, 1, "G5"), (3, 1, "E5")],
    [(0, 1, "F5"), (1, 0.5, "E5"), (1.5, 0.5, "D5"), (2, 2, "C5")],
]


def morning_pastries():
    form = BOSSA_A + BOSSA_A + BOSSA_B + BOSSA_A
    tr = Track(104, 4, len(form))
    for bar, name in enumerate(form):
        root, fifth, tones = BOSSA_CHORDS[name]
        b = bar * 4
        tr.note("bass", bass, b, 1.4, root, 0.9)
        tr.note("bass", bass, b + 2, 1.4, fifth, 0.8)
        hits = [(0, 0.9), (1.5, 0.45), (3, 0.9)] if bar % 2 == 0 else [(0.5, 0.45), (2, 0.9), (3.5, 0.45)]
        for (at, length) in hits:
            for k, tone in enumerate(tones):
                tr.note("guitar", nylon, b + at + k * 0.012, length, tone, 0.32)
        for e in range(8):
            tr.add("shaker", shaker(0.9 if e % 2 else 0.55), b + e * 0.5)
        for at in ([0, 1.5, 3] if bar % 2 == 0 else [1, 2.5]):
            tr.add("rim", rim(0.25), b + at)
    for start, mel in ((0, BOSSA_MEL_A), (8, BOSSA_MEL_A), (16, BOSSA_MEL_B), (24, BOSSA_MEL_A)):
        melody(tr, "vibes", vibes, start, mel, 0.5)
    return mix(tr, {"bass": 0.45, "guitar": 1.0, "shaker": 0.6, "rim": 1.0, "vibes": 1.15}, wet=0.22, tone=9000)


# ------------------------------------------------------------------ 2. Sugar & Sprinkles (bright, C major)

POP = {"C": ("C3", ["E4", "G4", "C5"]), "G/B": ("B2", ["D4", "G4", "B4"]), "Am": ("A2", ["E4", "A4", "C5"]),
       "F": ("F2", ["F4", "A4", "C5"]), "G": ("G2", ["D4", "G4", "B4"]), "Em": ("E3", ["E4", "G4", "B4"]), "Dm": ("D3", ["D4", "F4", "A4"])}
POP_A = ["C", "G/B", "Am", "F", "C", "G", "F", "G"]
POP_B = ["F", "G", "Em", "Am", "Dm", "G", "C", "G"]
POP_MEL_A = [
    [(0, .5, "E5"), (.5, .5, "G5"), (1, 1, "C6"), (2, .5, "G5"), (2.5, .5, "E5"), (3, 1, "G5")],
    [(0, .5, "D5"), (.5, .5, "G5"), (1, 1, "B5"), (2, .5, "A5"), (2.5, .5, "G5"), (3, 1, "D5")],
    [(0, .5, "C5"), (.5, .5, "E5"), (1, 1, "A5"), (2, .5, "G5"), (2.5, .5, "E5"), (3, 1, "C5")],
    [(0, 1, "A4"), (1, .5, "C5"), (1.5, 1.5, "F5"), (3, .5, "E5"), (3.5, .5, "D5")],
    [(0, 1, "E5"), (1, 1, "G5"), (2, .5, "E5"), (2.5, .5, "D5"), (3, 1, "C5")],
    [(0, 1, "D5"), (1, .5, "B4"), (1.5, .5, "D5"), (2, 2, "G5")],
    [(0, .5, "A5"), (.5, .5, "G5"), (1, .5, "F5"), (1.5, .5, "E5"), (2, 1, "D5"), (3, 1, "C5")],
    [(0, 1.5, "D5"), (1.5, .5, "B4"), (2, 2, "G4")],
]
POP_MEL_B = [
    [(0, 1, "C6"), (1, 1, "A5"), (2, 1, "F5"), (3, 1, "A5")],
    [(0, 1, "B5"), (1, 1, "G5"), (2, 1, "D5"), (3, 1, "G5")],
    [(0, .5, "G5"), (.5, .5, "B5"), (1, 1.5, "E6"), (2.5, .5, "D6"), (3, 1, "B5")],
    [(0, 1.5, "C6"), (1.5, .5, "B5"), (2, 2, "A5")],
    [(0, .5, "F5"), (.5, .5, "A5"), (1, 1, "D6"), (2, .5, "C6"), (2.5, .5, "A5"), (3, 1, "F5")],
    [(0, 1, "G5"), (1, 1, "B5"), (2, 1, "D6"), (3, 1, "B5")],
    [(0, .5, "C6"), (.5, .5, "G5"), (1, .5, "E5"), (1.5, .5, "G5"), (2, 2, "C6")],
    [(0, 1, "B5"), (1, .5, "A5"), (1.5, .5, "G5"), (2, 1, "D5"), (3, 1, "G5")],
]


def sugar_and_sprinkles():
    form = POP_A + POP_A + POP_B + POP_A
    tr = Track(116, 4, len(form))
    for bar, name in enumerate(form):
        root, tones = POP[name]
        b = bar * 4
        up = root[:-1] + str(int(root[-1]) + 1)
        for (at, n, v) in ((0, root, 0.9), (1.5, up, 0.6), (2, root, 0.8), (3.5, up, 0.6)):
            tr.note("bass", bass, b + at, 0.45, n, v)
        for e in (0.5, 1.5, 2.5, 3.5):
            for tone in tones:
                tr.note("pluck", pluck, b + e, 0.3, tone, 0.28)
        tr.add("kick", kick(0.8), b)
        tr.add("kick", kick(0.6), b + 2)
        tr.add("clap", clap(0.7), b + 1)
        tr.add("clap", clap(0.7), b + 3)
        for e in range(8):
            tr.add("shaker", shaker(0.8 if e % 2 else 0.5), b + e * 0.5)
    for start, mel in ((0, POP_MEL_A), (8, POP_MEL_A), (16, POP_MEL_B), (24, POP_MEL_A)):
        melody(tr, "marimba", marimba, start, mel, 0.6)
    melody(tr, "glock", glock, 16, POP_MEL_B, 0.22)
    melody(tr, "glock", glock, 8, POP_MEL_A, 0.14, transpose=octave_up)
    return mix(tr, {"bass": 0.45, "pluck": 0.8, "kick": 0.55, "clap": 0.5, "shaker": 0.5, "marimba": 1.15, "glock": 0.8}, wet=0.16)


# ------------------------------------------------------------------ 3. Rainy Window Latte (lo-fi)

LOFI = {"Gm9": ("G2", ["F3", "Bb3", "D4", "A4"]), "C9": ("C3", ["E3", "Bb3", "D4", "G4"]), "Fmaj9": ("F2", ["E3", "A3", "C4", "G4"]),
        "Bbmaj7": ("Bb2", ["A3", "D4", "F4"]), "Em7b5": ("E2", ["D3", "G3", "Bb3"]), "A7b9": ("A2", ["G3", "C#4", "Bb3", "E4"]),
        "Dm9": ("D3", ["C4", "F4", "A4", "E5"])}
LOFI_FORM = ["Gm9", "C9", "Fmaj9", "Bbmaj7", "Em7b5", "A7b9", "Dm9", "Dm9"]
LOFI_MEL = [
    [(1, 1, "D5"), (2, .5, "F5"), (2.5, 1.5, "A5")],
    [(0, 1, "G5"), (1, .5, "E5"), (1.5, 2, "D5")],
    [(.5, .5, "C5"), (1, .5, "E5"), (1.5, .5, "G5"), (2, 2, "A5")],
    [(0, 2, "F5"), (2, 1, "D5"), (3, 1, "C5")],
    [(0, 1, "Bb4"), (1, 1, "D5"), (2, 1.5, "G5")],
    [(0, 1, "F5"), (1, 1, "E5"), (2, 2, "C#5")],
    [(0, 1, "E5"), (1, .5, "F5"), (1.5, 1.5, "A5"), (3, 1, "C6")],
    [(0, 3, "A5")],
]


def rainy_window_latte():
    form = LOFI_FORM * 3
    tr = Track(76, 4, len(form))
    for bar, name in enumerate(form):
        root, tones = LOFI[name]
        b = bar * 4
        for k, tone in enumerate(tones):
            tr.note("keys", epiano, b + k * 0.03, 3.6, tone, 0.3)
        tr.note("bass", bass, b, 1.6, root, 0.85)
        tr.note("bass", bass, b + 2.5, 1.2, root, 0.6)
        tr.add("kick", kick(0.6), b)
        tr.add("kick", kick(0.45), b + 2.5)
        tr.add("snare", snare_soft(0.5), b + 1)
        tr.add("snare", snare_soft(0.5), b + 3)
        for e in range(8):
            swing = 0.08 if e % 2 else 0
            tr.add("hat", hat(0.8 if e % 2 else 1.0), b + e * 0.5 + swing, jitter=0.008)
    melody(tr, "lead", epiano, 0, LOFI_MEL, 0.42)
    melody(tr, "lead", vibes, 16, LOFI_MEL, 0.32)
    # the middle pass only has a few soft bells
    for bar, (beat, note) in ((9, (2, "A5")), (11, (1, "G5")), (13, (2.5, "F5")), (15, (0, "E5"))):
        tr.note("lead", glock, bar * 4 + beat, 1, note, 0.18)
    # gentle rain and a little vinyl crackle
    rain = lowpass(highpass(noise(tr.n), 400), 2500) * 0.035
    crackle = np.zeros(tr.n)
    idx = RNG.integers(0, tr.n, size=int(tr.n / SR * 6))
    crackle[idx] = RNG.uniform(0.05, 0.25, size=len(idx)) * RNG.choice([-1, 1], size=len(idx))
    tr.buses["rain"] = rain + lowpass(crackle, 6000)
    return mix(tr, {"keys": 1.0, "bass": 0.45, "kick": 0.55, "snare": 0.45, "hat": 0.5, "lead": 1.6, "rain": 1.0}, wet=0.25, tone=4200)


# ------------------------------------------------------------------ 4. Kitten Lullaby (music-box waltz, F major)

WALTZ = {"F": ("F3", ["A4", "C5"]), "C/E": ("E3", ["G4", "C5"]), "Dm": ("D3", ["F4", "A4"]), "Am/C": ("C3", ["E4", "A4"]),
         "Bb": ("Bb2", ["F4", "Bb4"]), "F/A": ("A2", ["F4", "C5"]), "Gm7": ("G2", ["F4", "Bb4"]), "C7": ("C3", ["E4", "Bb4"]),
         "C": ("C3", ["E4", "G4"])}
WALTZ_A = ["F", "C/E", "Dm", "Am/C", "Bb", "F/A", "Gm7", "C7"]
WALTZ_B = ["Dm", "Bb", "F", "C", "Bb", "C", "F", "C7"]
WALTZ_MEL_A = [
    [(0, 1, "C5"), (1, 1, "F5"), (2, 1, "A5")], [(0, 2, "G5"), (2, 1, "E5")], [(0, 1, "F5"), (1, 1, "A5"), (2, 1, "D6")],
    [(0, 2, "C6"), (2, 1, "A5")], [(0, 1, "Bb5"), (1, 1, "A5"), (2, 1, "G5")], [(0, 1, "A5"), (1, 1, "G5"), (2, 1, "F5")],
    [(0, 1.5, "G5"), (1.5, .5, "A5"), (2, 1, "Bb5")], [(0, 2, "C6"), (2, 1, "E5")],
]
WALTZ_MEL_B = [
    [(0, 1, "D6"), (1, 1, "C6"), (2, 1, "A5")], [(0, 2, "Bb5"), (2, 1, "F5")], [(0, 1, "A5"), (1, 1, "G5"), (2, 1, "F5")],
    [(0, 2, "E5"), (2, 1, "G5")], [(0, 1, "F5"), (1, 1, "G5"), (2, 1, "A5")], [(0, 1, "Bb5"), (1, 1, "A5"), (2, 1, "G5")],
    [(0, 1.5, "A5"), (1.5, .5, "G5"), (2, 1, "F5")], [(0, 1, "E5"), (1, 1, "G5"), (2, 1, "C5")],
]


def kitten_lullaby():
    form = WALTZ_A + WALTZ_B + WALTZ_A + WALTZ_B
    tr = Track(84, 3, len(form))
    for bar, name in enumerate(form):
        root, tones = WALTZ[name]
        b = bar * 3
        tr.note("bass", music_box, b, 1, root, 0.35)
        for beat in (1, 2):
            for tone in tones:
                tr.note("box", music_box, b + beat, 0.8, tone, 0.16)
        for tone in tones:
            tr.note("pad", pad, b, 2.9, tone, 0.5)
    for start, mel in ((0, WALTZ_MEL_A), (8, WALTZ_MEL_B), (16, WALTZ_MEL_A), (24, WALTZ_MEL_B)):
        melody(tr, "melody", music_box, start, mel, 0.55)
    melody(tr, "melody", glock, 16, WALTZ_MEL_A, 0.12, transpose=octave_up)
    return mix(tr, {"bass": 0.9, "box": 0.8, "pad": 0.6, "melody": 1.0}, wet=0.3, tone=8000)


TRACKS = {"morning_pastries": morning_pastries, "sugar_and_sprinkles": sugar_and_sprinkles,
          "rainy_window_latte": rainy_window_latte, "kitten_lullaby": kitten_lullaby}


def build(only=None):
    out = {}
    for name, make in TRACKS.items():
        if only and name not in only:
            continue
        out[name] = write(name, make())
    return out


if __name__ == "__main__":
    for name, seconds in build(sys.argv[1:]).items():
        print(f"{name}: {seconds:.1f}s")
