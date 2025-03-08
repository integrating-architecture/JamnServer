/* Authored by www.integrating-architecture.de */

let infos = [
	["Graal.versionECMAScript" , Graal.versionECMAScript],
	["Graal.versionGraalVM" , Graal.versionGraalVM]
];

[...infos.values()].map(e => e.join(' = ')).join('\n');
