/**
 * Tools povides some helper functions
 * in particular functions provided by the HostApp
 */

export const version = "0.0.1"; 

//actual system line separator
export const LS = HostApp.ls()

/**
 * call command in os shell
 * e.g. a os command or a shell script
 */
export function sh(command, workDir="") {
	let result = HostApp.shellCmd(command, workDir);
	//convert java type List to javaScript type array
	return Java.from(result);
};

/**
 * get a path string as os dependend path
 */
export function path(path){
	return HostApp.path(path);
};
