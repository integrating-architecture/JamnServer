/* Authored by www.integrating-architecture.de */

import { GeneralView } from '../jsmod/view-classes.mjs';
import { getChildOf, setDisplay } from '../jsmod/tools.mjs';

/**
 * A playground implementation
 */

//export this view component as singleton instance
const viewInstance = new GeneralView("systemLoginView", "/jsmod/login.html");
export function getView() {
	return viewInstance;
}

let tries = 0;
let accessToken = null;

/**
 */
export function isLoggedIn() {
	return accessToken != null;
}

/**
 */
export function processSystemLogin(dlg) {

	if (isLoggedIn()) {
		WbApp.confirm({
			title: "Confirmation required",
			message: "<b>Log Off</b><br>Do you want to Log Off from the Server System?"
		}, (value) => value ? doLogOff() : null);
	} else {
		tries = 0;
		dlg.setTitle("Jamn System LogIn")
			.setAction("pb.login", () => dialogAction(dlg))
			.setAction("pb.login.success", () => dlg.close())
			.open();
	}
}

/**
 */
function dialogAction(dlg) {

	let logInData = {
		username: "username",
		password: "password"
	};

	for (let key in logInData) {
		logInData[key] = getChildOf(dlg.viewArea, logInData[key])?.value;
	}

	if (doLogin(logInData.username, logInData.password)) {
		dlg.close();
	} else {
		let msgElem = getChildOf(dlg.viewArea, "message");
		if (tries < 2) {
			msgElem.innerHTML = `Sorry, unfortunately this login [${tries}] failed for test reasons.<br>But please try again ...`;
		} 
		setDisplay(msgElem, true);
		if (tries > 1) {
			//display success message as dialog overlay
			setDisplay(getChildOf(dlg.viewArea, "login.overlay"), "inline-block");
			toggleStatus();
		}
	}
}

function toggleStatus() {
	let triggerElem = null;
	if (!isLoggedIn()) {
		triggerElem = document.getElementById("sidebar.header.login.icon");
		triggerElem.classList.remove("bi-person");
		triggerElem.classList.add("bi-person-check");
		triggerElem.style.color = "green";
		triggerElem.title = "Log Off";

		triggerElem = document.getElementById("sidebar.system.login");
		triggerElem.innerHTML = "Log Off";

		accessToken = "jwt bearer";
	} else {
		triggerElem = document.getElementById("sidebar.header.login.icon");
		triggerElem.classList.remove("bi-person-check");
		triggerElem.classList.add("bi-person");
		triggerElem.style.color = "inherit";
		triggerElem.title = "Login";

		triggerElem = document.getElementById("sidebar.system.login");
		triggerElem.innerHTML = "Login";

		accessToken = null;
	}
}

/**
 */
function doLogin(user, password) {
	console.log(`LogIn Action for - ${user}`);
	tries++;
	return false;
}

/**
 */
function doLogOff() {
	toggleStatus();
}
