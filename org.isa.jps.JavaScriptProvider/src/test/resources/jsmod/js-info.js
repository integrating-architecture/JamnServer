const info = [
	["Graal.versionECMAScript" , Graal.versionECMAScript],
	["Graal.versionGraalVM" , Graal.versionGraalVM],
	["Graal.isGraalRuntime" , Graal.isGraalRuntime]
];

print([...info.values()].map(e => e.join(' = ')).join('\n'));
