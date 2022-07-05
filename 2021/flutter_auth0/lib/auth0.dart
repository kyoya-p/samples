// @dart=2.9

import 'package:auth0_flutter_web/auth0_flutter_web.dart';

auth0() async {
  Auth0 auth0 = await createAuth0Client(Auth0CreateOptions(
    domain: 'shokkaa.jp.auth0.com',
    client_id: 'xzvz2UtFRONnReaZwprNsVmTvFU5d5W5',
  ));
  await auth0.loginWithPopup();
}
