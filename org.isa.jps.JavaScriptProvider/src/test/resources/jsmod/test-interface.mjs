
const IFaceClass = Java.type("org.isa.jps.JavaScriptExtensionTest.JavaTestInterfaceClass");

export const testIFace = new IFaceClass();

export function appName(){
	return testIFace.appName();
};

export function shellCmd(cmdparts, workingDir){
	return testIFace.shellCmd(cmdparts, workingDir);
};

var functions = {appName, shellCmd};
functions;