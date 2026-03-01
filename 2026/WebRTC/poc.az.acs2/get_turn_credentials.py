import os
import json
import time
from dotenv import load_dotenv
from azure.communication.networktraversal import CommunicationRelayClient
from azure.communication.identity import CommunicationIdentityClient
from azure.core.exceptions import HttpResponseError

# .env ファイルから接続文字列を読み込む
load_dotenv()

def get_turn_credentials():
    connection_string = os.getenv("ACS_CONNECTION_STRING")
    if not connection_string:
        print("Error: ACS_CONNECTION_STRING is not set in .env file.")
        return

    try:
        # 1. 通信ユーザーの作成
        identity_client = CommunicationIdentityClient.from_connection_string(connection_string)
        user = identity_client.create_user()
        user_id = user.properties['id']
        print(f"Created Identity: {user_id}")

        # 2. Relay Client の初期化
        relay_client = CommunicationRelayClient.from_connection_string(connection_string)

        # 3. TURN 資格情報の取得 (リトライ付き)
        print("Fetching TURN credentials (may take a few minutes for new resources)...")
        max_retries = 10
        for i in range(max_retries):
            try:
                relay_config = relay_client.get_relay_configuration(user=user)
                
                # WebRTC の RTCConfiguration.iceServers 形式に変換
                ice_servers = []
                for server in relay_config.ice_servers:
                    ice_servers.append({
                        "urls": server.urls,
                        "username": server.username,
                        "credential": server.credential
                    })

                # 結果を JSON で出力
                print("\n--- iceServers ---")
                print(json.dumps({"iceServers": ice_servers}, indent=2))
                return # 成功したら終了

            except Exception as e:
                print(f"Attempt {i+1}/{max_retries} failed: {e}")
                if i < max_retries - 1:
                    print("Retrying in 60 seconds...")
                    time.sleep(60)
                else:
                    print("Max retries reached. Please wait a few more minutes and try again.")

    except HttpResponseError as e:
        print(f"Azure HTTP Response Error: {e.status_code}")
        print(f"Message: {e.message}")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    get_turn_credentials()
