/* Authored by www.integrating-architecture.de */

/**
 * Some simple helper constants and functions
 */

export const NL = "\n";

/**
 */
export function ServerOrigin(...path) {
	let url = window.location.origin;
	let urlPath = path.join("/");
	if (urlPath.startsWith("/")) {
		url = url + urlPath;
	} else {
		url = url + "/" + urlPath;
	}
	return url;
}

/**
 */
export function getChildOf(parent, childId) {
	if (typeof parent === 'string') {
		parent = document.getElementById(parent);
	}

	let innerElem = null;
	for (let elem of parent.childNodes) {
		if (elem?.id == childId) {
			return elem;
		} else {
			innerElem = getChildOf(elem, childId);
			if (innerElem?.id == childId) {
				return innerElem;
			}
		}
	}
	return null;
}

/**
 */
export function setVisibility(elem, flag) {
	elem.style["visibility"] = flag ? "visible" : "hidden";
	return elem;
}

export function setDisplay(elem, flag) {
	if (typeof flag == "boolean") {
		elem.style["display"] = flag ? "block" : "none";
	} else if (typeof flag == "string") {
		elem.style["display"] = flag;
	}
	return elem;
}

/**
 * Get a view html file 
 */
export function getViewHtml(viewSrc, cb) {
	if (viewSrc.html) {
		cb(viewSrc.html);
	} else {
		//load the html from server
		fetchPlainText(viewSrc.file).then((html) => {
			viewSrc.html = html;
			cb(viewSrc.html);
		});
	}
}

/**
 */
export async function fetchPlainText(path) {
	const url = ServerOrigin(path);
	let data = "";

	const response = await fetch(url, {
		method: "GET",
		accept: "text/plain",
		headers: { "Content-Type": "text/plain" },
		mode: "cors" // required for localhost communication via js fetch
	})

	data = await response.text();

	return data;
}

/**
 */
export async function callWebService(path, requestData = "{}") {
	const url = ServerOrigin(path);
	let data = "";

	const response = await fetch(url, {
		method: "POST",
		accept: "application/json",
		headers: { "Content-Type": "application/json" },
		mode: "cors", // required for localhost communication via js fetch
		body: requestData
	})

	data = await response.text();
	data = JSON.parse(data);
	return data;
}

/**
 */
export function newSimpleId(prfx = "") {
	return prfx + Math.random().toString(16).slice(2);
}

/**
 */
export const typeUtil = {

	isString: (val) => {
		return (typeof val === 'string' || val instanceof String);
	},

	isObject(val) {
		return (val !== null && typeof val === 'object');
	},

	isFunction(val) {
		return (val !== null && (typeof val === 'function' || val instanceof Function));
	},

	isNumber(val) {
		return (val !== null && typeof val === 'number');
	},

	isBoolean: (val) => {
		return (val === true || val === false);
	},

	isBooleanString: (val) => {
		return (val === "true" || val === "false");
	},

	booleanFromString: (val) => {
		if (typeUtil.isBooleanString(val)) {
			return (val === "true");
		}
		return null;
	},

	stringFromBoolean: (val) => {
		if (typeUtil.isBoolean(val)) {
			return val ? "true" : "false";
		}
		return null;
	}

}