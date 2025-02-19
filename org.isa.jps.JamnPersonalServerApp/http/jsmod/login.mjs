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
			title: "",
			message: "<b>Log Off requested</b><br>Do you really want to Log Off from the Server System?"
		}, (choice) => choice === "yes" ? doLogOff() : null);
	} else {
		tries = 0;
		dlg.setTitle("Jamn System LogIn")
			.setAction("pb.login", () => dialogAction(dlg))
			.setAction("pb.success", () => dlg.close())
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
			msgElem.innerHTML = `Sorry, unfortunately the login failed [${tries}]<br>Please try again ...`;
		} else {
			msgElem.innerHTML = `Ups ... login failed again [${tries}]<br>but you have still one try left ...`;
		}
		setDisplay(msgElem, true);
		if (tries > 2) {
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
