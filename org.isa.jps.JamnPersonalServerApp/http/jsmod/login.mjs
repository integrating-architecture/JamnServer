/* Authored by iqbserve.de */

import { AbstractView, onClicked } from '../jsmod/view-classes.mjs';
import { installSidebarHeaderWorkItem } from '../jsmod/sidebar.mjs';
import { WorkbenchInterface as WbApp } from '../jsmod/workbench.mjs';
import * as Icons from '../jsmod/icons.mjs';

/**
 * An example LogIn module based on a html source file shown in a modal dialog.
 */
class LoginView extends AbstractView {

	username;
	password;
	message;
	successMessage;

	initialize() {
		this.username = this.getElement("username");
		this.password = this.getElement("password");
		this.message = this.getElement("message");
		this.successMessage = this.getElement("login.overlay");

		onClicked(Icons.user(this.getElement("lbUsername")).elem, () => { this.username.value = ""; });
		Icons.password(this.getElement("lbPassword"));
		Icons.loginAction(this.getElement("login.action.icon"));

		this.isInitialized = true;
	}

	showMessage(msgText, showFlag) {
		this.message.innerHTML = showFlag ? msgText : "";
		this.setDisplay(this.message, showFlag);
	}

	showSuccessMessage() {
		this.setDisplay(this.successMessage, "inline-block");
	}

	reset() {
		this.username.value = "";
		this.password.value = "";
		this.showMessage("", false);
		this.setDisplay(this.successMessage, false);
	}
}

/**
 */
let tries = 0;
let accessToken = null;
//create the view instance based on a html source file
let viewInstance = new LoginView("systemLoginView", "/jsmod/html-components/login.html");

//install login icon in sidebar header
let sidebarLoginIcon = installSidebarHeaderWorkItem({
	id: "sidebar.login.icon", iconName: Icons.login(), title: "Login",
	action: (evt) => {
		processSystemLogin();
	}
});

/**
 */
export function isLoggedIn() {
	return accessToken != null;
}

/**
 */
export function processSystemLogin() {

	if (isLoggedIn()) {
		WbApp.confirm({
			message: "<b>Log Off</b><br>Do you want to Log Off from the Server System?"
		}, (value) => value ? doLogOff() : null);
	} else {
		WbApp.modalDialog(viewInstance, (dialog) => {
			viewInstance.reset();
			tries = 0;
			dialog.setTitle("Jamn System LogIn")
				.setAction("pb.login", () => dialogLoginAction(dialog))
				.setAction("pb.login.success", () => closeLogin(dialog))
				.open();
		});
	}
}

/**
 * Internals
 */
/**
 */
function dialogLoginAction(dialog) {

	if (doLogin()) {
		closeLogin(dialog);
	} else {
		if (tries < 2) {
			let text = `We are sorry, unfortunately this login [${tries}] failed for demo reason.<br><b>Please click again ...`;
			viewInstance.showMessage(text, true);
		}
		if (tries > 1) {
			viewInstance.showSuccessMessage();
			toggleStatus();
		}
	}
}

/**
 */
function closeLogin(dialog) {
	dialog.close();
	viewInstance.reset();
}

/**
 */
function doLogin() {
	console.log(`LogIn Action for - ${viewInstance.username.value}`);
	tries++;
	//demo always false
	return false;
}

/**
 */
function doLogOff() {
	toggleStatus();
}

/**
 */
function toggleStatus() {

	sidebarLoginIcon.toggle((icon) => {
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
