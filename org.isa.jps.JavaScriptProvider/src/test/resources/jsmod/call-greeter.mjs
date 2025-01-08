import {Greeter} from 'greeter.mjs';

const greeter = new Greeter();
const greeting = greeter.sayHello('User');

//const caseInsensitiveSort = (a, b) => {
//  const lowerA = a.toLowerCase();
//  const lowerB = b.toLowerCase();
//  if (lowerA < lowerB) return -1;
//  if (lowerA > lowerB) return 1;
//  return 0;
//};
//
//const sortedGlobals = [...Object.getOwnPropertyNames(globalThis)].sort(caseInsensitiveSort);
//
//print(sortedGlobals.join("\n"));
//
//print(Graal.versionECMAScript);
//print(Graal.versionGraalVM);
//print(Graal.isGraalRuntime());

const info = [
	["Graal.versionECMAScript" , Graal.versionECMAScript],
	["Graal.versionGraalVM" , Graal.versionGraalVM],
	["Graal.isGraalRuntime" , Graal.isGraalRuntime]
];

print([...info.values()].map(e => e.join(' = ')).join('\n'));
greeting;
