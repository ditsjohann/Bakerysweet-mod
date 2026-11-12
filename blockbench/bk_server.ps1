# Art pipeline without Blockbench: serves a page that runs the bk_*.js painter scripts in a browser
# and lets them read and write files inside the mod folder (and nowhere else).
# Run: powershell -ExecutionPolicy Bypass -File blockbench/bk_server.ps1   (then open http://localhost:8765/)
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$listener = New-Object System.Net.HttpListener
$port = if ($env:PORT) { $env:PORT } else { '8765' }
$listener.Prefixes.Add("http://localhost:$port/")
$listener.Start()
Write-Host "bakery art server on http://localhost:$port/ for $root"

$page = @'
<!doctype html><html><head><meta charset="utf-8"><title>Bakery art</title></head>
<body style="background:#fdf2f6;font-family:sans-serif"><h3>Sweetheart Bakery art pipeline</h3><div id="log"></div>
<script>
function call(method, url, body) {
  const x = new XMLHttpRequest(); x.open(method, url, false);
  x.send(body === undefined ? null : body);
  if (x.status >= 400) throw new Error(method + ' ' + url + ' -> ' + x.status + ' ' + x.responseText);
  return x.responseText;
}
const q = p => encodeURIComponent(String(p).replace(/\\/g, '/'));
const FS = {
  readFileSync: p => call('GET', '/read?path=' + q(p)),
  existsSync: p => call('GET', '/exists?path=' + q(p)) === 'true',
  mkdirSync: () => {},
  writeFileSync: (p, data) => {
    if (data && data.__b64 !== undefined) call('POST', '/write?b64=1&path=' + q(p), data.__b64);
    else call('POST', '/write?path=' + q(p), typeof data === 'string' ? data : (data && data.__text) || '');
  },
  readdirSync: p => JSON.parse(call('GET', '/list?path=' + q(p))),
  unlinkSync: p => call('POST', '/delete?path=' + q(p), '')
};
const PATH = {
  join: (...a) => a.join('/').replace(/\\/g, '/').replace(/\/+/g, '/'),
  dirname: p => String(p).replace(/\\/g, '/').replace(/\/[^/]*$/, ''),
  basename: p => String(p).replace(/\\/g, '/').replace(/^.*\//, '')
};
window.require = n => n === 'fs' ? FS : n === 'path' ? PATH : null;
window.Buffer = { from: (s, enc) => enc === 'base64' ? { __b64: s } : { __text: s } };
window.run = file => (0, eval)(FS.readFileSync(file));
document.getElementById('log').textContent = 'ready';
</script></body></html>
'@

function Resolve-Safe([string]$p) {
	$full = [System.IO.Path]::GetFullPath(($p -replace '/', '\'))
	if (-not $full.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { throw "outside mod folder: $full" }
	return $full
}

while ($listener.IsListening) {
	$ctx = $listener.GetContext()
	$req = $ctx.Request; $res = $ctx.Response
	$status = 200; $out = ''; $bytes = $null
	try {
		$path = $req.QueryString['path']
		switch ($req.Url.AbsolutePath) {
			'/raw' { $bytes = [System.IO.File]::ReadAllBytes((Resolve-Safe $path)); $res.ContentType = 'image/png' }
			'/' { $out = $page; $res.ContentType = 'text/html; charset=utf-8' }
			'/read' { $out = [System.IO.File]::ReadAllText((Resolve-Safe $path)) }
			'/exists' { $out = if (Test-Path -LiteralPath (Resolve-Safe $path)) { 'true' } else { 'false' } }
			'/list' { $out = ConvertTo-Json -Compress @(Get-ChildItem -LiteralPath (Resolve-Safe $path) | ForEach-Object { $_.Name }) }
			'/delete' { Remove-Item -LiteralPath (Resolve-Safe $path) -Force; $out = 'ok' }
			'/write' {
				$file = Resolve-Safe $path
				$reader = New-Object System.IO.StreamReader($req.InputStream, [System.Text.Encoding]::UTF8)
				$body = $reader.ReadToEnd()
				New-Item -ItemType Directory -Force -Path ([System.IO.Path]::GetDirectoryName($file)) | Out-Null
				if ($req.QueryString['b64'] -eq '1') { [System.IO.File]::WriteAllBytes($file, [Convert]::FromBase64String($body)) }
				else { [System.IO.File]::WriteAllText($file, $body, (New-Object System.Text.UTF8Encoding($false))) }
				$out = 'ok'
			}
			default { $status = 404; $out = 'not found' }
		}
	} catch { $status = 500; $out = $_.Exception.Message; $bytes = $null }
	if ($null -eq $bytes) { $bytes = [System.Text.Encoding]::UTF8.GetBytes($out) }
	$res.StatusCode = $status
	$res.ContentLength64 = $bytes.Length
	$res.OutputStream.Write($bytes, 0, $bytes.Length)
	$res.OutputStream.Close()
}
