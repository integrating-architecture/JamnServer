/* Authored by iqbserve.de */

import { ViewDialog, loadServerStyleSheet } from '../jsmod/view-classes.mjs';
import { onClicked, reworkHtmlElementIds } from '../jsmod/uibuilder.mjs';
import { WorkbenchInterface as WbApp } from '../jsmod/workbench.mjs';
import * as Icons from '../jsmod/icons.mjs';

/**
 * LogIn module based on a html source file shown in a modal dialog.
 */
class LoginDialog extends ViewDialog {

	constructor() {
		super("/jsmod/html-components/login.html");
	}

	reworkHtml(html) {
		//make the ids of the html source local
		html = reworkHtmlElementIds(html, this.uid.get());
		return html;
	}

	beforeCreateViewElement() {
		loadServerStyleSheet("/jsmod/html-components/login.css");
	}

	initialize() {
		super.initialize();

		this.elementsToProperties(["username", "password", "message", "successMessage"]);

		onClicked(Icons.user(this.getElement("lbUsername")).elem, () => { this.username.value = ""; });
		Icons.password(this.getElement("lbPassword"));
		Icons.loginAction(this.getElement("login-action-icon"));

		this.setTitle("Jamn System LogIn")
		this.setAction("pbLogin", () => dialogLoginAction(this))
			.setAction("pbLoginSuccess", () => this.close());

		this.dialog().style["margin-top"] = ViewDialog.default.styleProps["margin-top"];
	}

	getElement(id) {
		//overwritten - to switch to local context ids
		return super.getElement(this.uid.get(id));
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

	beforeOpen() {
		this.reset();
	}

}

/**
 */
let tries = 0;
let accessToken = null;

//create a dialog instance based on a html source file
let dialog = new LoginDialog();

let sidebarLoginIcon = Icons.newIcon(Icons.login(), document.getElementById("sidebar.icon.login"));
let sidebarLoginItem = document.getElementById("sidebar.item.login");

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
		tries = 0;
		dialog.open();
	}
}

/**
 * Internals
 */
/**
 */
function dialogLoginAction(dialog) {

	if (doLogin()) {
		dialog.close();
	} else {
		if (tries < 2) {
			let text = `We are sorry, unfortunately this login [${tries}] failed for demo reason.<br><b>Please click again ...`;
			dialog.showMessage(text, true);
		}
		if (tries > 1) {
			dialog.showSuccessMessage();
			toggleStatus();
		}
	}
}

/**
 */
function doLogin() {
	console.log(`LogIn Action for - ${dialog.username.value}`);
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
			sidebarLoginItem.innerHTML = "Log Off";

			accessToken = "jwt bearer";
		} else {
			icon.style.color = "";
			icon.title = "Login";
			sidebarLoginItem.innerHTML = "Login";

			accessToken = null;
		}
	});
}
