
info = [
	["Graal.versionECMAScript" , Graal.versionECMAScript],
	["Graal.versionGraalVM" , Graal.versionGraalVM]
];

val = [...info.values()].map(e => e.join(' = ')).join('\n');