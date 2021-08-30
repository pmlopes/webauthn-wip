const signin = document.getElementById('signin');

const loginButton = document.getElementById('loginButton');
const registerButton = document.getElementById('registerButton');
const errorArea = document.getElementById('errorArea');

const displayErrorMessage = message => {
  errorArea.innerHTML = message;
}

const w = new WebAuthn({
  callbackPath: '/webauthn/callback',
  registerPath: '/webauthn/register',
  loginPath: '/webauthn/login'
});

registerButton.onclick = () => {
  w
    .register({
      name: document.getElementById('register-name').value,
      displayName: document.getElementById('register-display-name').value
    })
    .then(() => {
      signin.checked = true;
    })
    .catch(err => {
      displayErrorMessage('registration failed!');
      console.error(err);
    });
};

loginButton.onclick = () => {
  w
    .login({
      name: document.getElementById('login-name').value
    })
    .then(() => {
      window.location.assign('/protected/index.html');
    })
    .catch(err => {
      displayErrorMessage('Invalid credential!');
      console.error(err);
    });
};
