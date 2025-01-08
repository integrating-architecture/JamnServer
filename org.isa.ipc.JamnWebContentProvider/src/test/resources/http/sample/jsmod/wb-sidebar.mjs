
/**
 */
export function initSideBar(topicids) {
  initTopics(topicids);
}


/**
 * add click behavior show/hide to sidebar topics
 */
function initTopics(ids) {
  let topics = document.getElementsByClassName("sbar-topic");

  for (let i = 0; i < topics.length; i++) {

    //
    topics[i].addEventListener("click", function (evt) {
      if (ids.includes(evt.target.id)) {
        let list = this.querySelector('.sbar-topic-entry-list');
        if (list) {
          if (list.style.display == "none" || list.style.display == "") {
            list.style.display = "block";
          } else {
            list.style.display = "none";
          }
        }
      }
    });
  }
} 