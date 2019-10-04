'use strict';

let getMakeCredentialsChallenge = (formBody) => {
  return fetch('/webauthn/register', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(formBody)
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`Server responded with error: ${response.statusText}`);
      }
      return response;
    })
    .then((response) => response.json())
};

let sendWebAuthnResponse = (body) => {
  return fetch('/webauthn/response', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`Server responded with error: ${response.statusText}`);
      }
      return response;
    })
};

/* Handle for register form submission */
document.getElementById('register').addEventListener('submit', function (event) {
  event.preventDefault();

  let name = this.username.value;
  let displayName = this.username.value;
  let type = this.type.value;

  if (!name || !displayName) {
    window.alert('DisplayName or username is missing!');
    return
  }

  // STEP 1 request createCredentialObject from server
  getMakeCredentialsChallenge({name, displayName, type})
    .then((response) => {
      console.log('Options for creating crendential', response);
      // STEP 5 convert challenge & id to buffer and perform register
      let publicKey = window.preformatMakeCredReq(response);
      clearAlert();
      document.getElementById('touch-alert').style.display = 'block';
      return navigator.credentials.create({publicKey})
    })
    .then((response) => {
      // STEP 6 convert response from buffer to json
      let makeCredResponse = window.publicKeyCredentialToJSON(response);
      console.log('Credential', makeCredResponse);
      // STEP 7 Send to server
      return sendWebAuthnResponse(makeCredResponse)
    })
    .then((response) => {
      clearAlert();
      document.getElementById('register-success-alert').style.display = 'block';
      console.log('Registration completed')
    })
    .catch((error) => window.alert(error))
});

let getGetAssertionChallenge = (formBody) => {
  // TODO: STEP 17 Start login flow
  return fetch('/webauthn/login', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(formBody)
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`Server responded with error: ${response.statusText}`);
      }
      return response;
    })
    .then((response) => response.json())
};

let clearAlert = () => {
  Array.from(document.getElementsByClassName('alert')).forEach(ele => {
    ele.style.display = 'none'
  })
};

/* Handle for login form submission */
document.getElementById('login').addEventListener('submit', function (event) {
  event.preventDefault();

  let name = this.username.value;

  if (!name) {
    window.alert('Username is missing!');
    return
  }

  getGetAssertionChallenge({name})
    .then((response) => {
      // STEP 20 perform get credential
      let publicKey = window.preformatGetAssertReq(response);
      clearAlert();
      document.getElementById('touch-alert').style.display = 'block';
      return navigator.credentials.get({publicKey})
    })
    .then((response) => {
      // STEP 21 convert response to json
      let getAssertionResponse = window.publicKeyCredentialToJSON(response);
      console.log('Assertion', getAssertionResponse);
      // STEP 22 send information to server
      return sendWebAuthnResponse(getAssertionResponse)
    })
    .then((response) => {
      clearAlert();
      document.getElementById('login-success-alert').style.display = 'block';
      console.log('Login success')
    })
    .catch((error) => window.alert(error))
});
