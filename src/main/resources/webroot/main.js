const loginButton = document.getElementById('login');
const registerButton = document.getElementById('register');
const messageDiv = document.getElementById('message');

const w = new WebAuthn({
  callbackPath: '/webauthn/callback',
  registerPath: '/webauthn/register',
  loginPath: '/webauthn/login'
});

const displayMessage = message => {
  messageDiv.innerHTML = message;
}

registerButton.onclick = () => {
  w
    .register({
      name: 'email@email.com',
      displayName: 'Paulo Lopes'
    })
    .then(() => {
      displayMessage('registration successful');
    })
    .catch(err => {
      displayMessage('registration failed');
      console.error(err);
    });
};

loginButton.onclick = () => {
  w
    .login({
      name: 'email@email.com'
    })
    .then(() => {
      displayMessage('You are logged in');
    })
    .catch(err => {
      displayMessage('Invalid credential');
      console.error(err);
    });
};
