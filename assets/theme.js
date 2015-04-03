function init() {
    var image = document.getElementById("userimage");
    image.src = lightdm.users[0].image;
}

function show_prompt(text) {

}

function selectUser(username) {

}

function login(password) {
    lightdm.start_authentication(lightdm.users[0].name);
    lightdm.provide_secret(password);
}

function authentication_complete() {
    lightdm.login(lightdm.users[0].name, "i3");
}
