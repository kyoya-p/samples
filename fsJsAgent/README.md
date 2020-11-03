Firebase Custom Token 認証
====

[参照:JavaScript でカスタム認証システムを使用して Firebase 認証を行う](https://firebase.google.com/docs/auth/web/custom-auth?hl=ja)

認証Sequence
----
https://sequencediagram.org/
```
device <-	operator:					Enter(serviceAccountCredentialInfo,decieId)

device ->	customSvr: 					makeCustomToken(deviceId,password)
			customSvr ->	Firebase:	authorize(serviceAccountCredentialInfo)
			customSvr ->	Firebase:	pw=getDevicePassword(deviceId)
            customSvr -> 	customSvr:	check password
            customSvr -> 	customSvr:	create custom claimes info
            customSvr -> 	Firebase:	createCustomToken()
device <--  customSvr:					customToken
device ->				 	Firebase:	loginWithCustomToken(custoToken)
```
