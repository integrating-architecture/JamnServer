/* Authored by www.integrating-architecture.de */

import { GeneralView, IconElement } from '../jsmod/view-classes.mjs';
import { getChildOf, setDisplay } from '../jsmod/tools.mjs';

/**
 * A playground implementation
 * just showing a login dialog
 */

//export a view component as singleton 
//in this case the view object is just the holder for the lazy loaded view html 
//used by the modal dialog
const viewInstance = new GeneralView("systemLoginView", "/jsmod/login.html");
export function getView() {
	return viewInstance;
}

let tries = 0;
let accessToken = null;
let sidebarLoginIcon = IconElement.newIcon("login", document.getElementById("sidebar.login.icon"));

/**
 */
export function isLoggedIn() {
	return accessToken != null;
}

/**
 */
export function processSystemLogin(dialog) {

	if (isLoggedIn()) {
		WbApp.confirm({
			title: "Confirmation required",
			message: "<b>Log Off</b><br>Do you want to Log Off from the Server System?"
		}, (value) => value ? doLogOff() : null);
	} else {
		tries = 0;
		dialog.setTitle("Jamn System LogIn")
			.setAction("pb.login", () => dialogAction(dialog))
			.setAction("pb.login.success", () => dialog.close())
			.setElement("user.icon", (elem)=>{IconElement.newIcon("user", elem);})
			.setElement("password.icon", (elem)=>{IconElement.newIcon("password", elem);})
			.setElement("login.action.icon", (elem)=>{IconElement.newIcon("loginAction", elem);})
			.open();
	}
}

/**
 */
function dialogAction(dialog) {

	let logInData = {
		username: "username",
		password: "password"
	};

	for (let key in logInData) {
		logInData[key] = getChildOf(dialog.viewArea, logInData[key])?.value;
	}

	if (doLogin(logInData.username, logInData.password)) {
		dialog.close();
	} else {
		let msgElem = getChildOf(dialog.viewArea, "message");
		if (tries < 2) {
			msgElem.innerHTML = `We are sorry, unfortunately this login [${tries}] failed for demo reason.<br><b>Please click again ...`;
		} 
		setDisplay(msgElem, true);
		if (tries > 1) {
			//display success message as dialog overlay
			setDisplay(getChildOf(dialog.viewArea, "login.overlay"), "inline-block");
			toggleStatus();
		}
	}
}

function toggleStatus() {

	sidebarLoginIcon.toggle().and((icon) => {
		if (!isLoggedIn()) {
			icon.style.color = "green";
			icon.title = "Log Off";
	
			document.getElementById("sidebar.system.login").innerHTML = "Log Off";
	
			accessToken = "jwt bearer";
		} else {
			icon.style.color = "";
			icon.title = "Login";
	
			document.getElementById("sidebar.system.login").innerHTML = "Login";
	
			accessToken = null;
		}	
	});
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
