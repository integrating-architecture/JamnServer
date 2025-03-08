/* Authored by www.integrating-architecture.de */

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
export function sh(command, workDir = "", outputConsumer = null) {
	let result = HostApp.shellCmd(command, workDir, outputConsumer);
	//convert java type List to javaScript type array
	return Java.from(result);
};

/**
 */
export function isOnUnix() {
	return HostApp.isOnUnix();
};

/**
 * echo/print/log text to java host app
 */
export function echo(text) {
	return HostApp.echo(text);
};

/**
 * get a path string as os dependend path
 */
export function path(...path) {
	return HostApp.path(path);
};

/**
 * get workspace path string 
 */
export function workspacePath(...path) {
	return HostApp.workspacePath(path);
};
