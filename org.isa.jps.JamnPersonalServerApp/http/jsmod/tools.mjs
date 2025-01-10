/**
 * Some simple helper functions
 */

export const serverOrigin = window.location.origin;

/**
 */
export function setHTML(id, html) {
	let elem = document.getElementById(id);
	elem.innerHTML = html;
}

export function setChildHTML(parentId, childId, html) {
	let elem = document.getElementById(parentId);
	if (elem) {
		elem = getChildOf(elem, childId);
		if (elem) {
			elem.innerHTML = html;
		}
	}
}

export function getChildOf(parent, childId) {
	let elem = null;
	for (let i = 0; i < parent.childNodes.length; i++) {
		elem = parent.childNodes[i];
		if (elem && elem.id == childId) {
			return elem;
		} else {
			elem = getChildOf(elem, childId);
			if (elem && elem.id == childId) {
				return elem;
			}
		}
	}
	return null;
}

/**
 */
export function setAttr(id, name, value) {
	let elem = document.getElementById(id);
	elem.setAttribute(name, value);
}

/**
 */
export function setStyle(id, name, value) {
	let elem = document.getElementById(id);
	elem.style[name] = value;
}

/**
 */
export async function fetchPlainText(path) {
	const url = serverOrigin + path;
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
export async function callWebService(path, requestData="{}") {
	const url = serverOrigin + path;
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
