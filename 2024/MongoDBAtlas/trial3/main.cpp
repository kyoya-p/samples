#include "cpprealm/sdk.hpp"

struct Todo // スキーマ定義
{
  realm::primary_key<realm::object_id> _id{realm::object_id::generate()};
  std::string summary;
  bool isComplete;
  std::string owner_id;
};

namespace realm
{
  REALM_SCHEMA(Todo, _id, summary, isComplete, owner_id);
}

// namespace realm
// {
//   REALM_SCHEMA(Todo, _id, summary, isComplete, owner_id);
// }

template <typename T>
void printItem(T item)
{
  std::cout << "Todo:{summary:" << (std::string)item.summary << ", isComplete:" << (bool)item.isComplete << "}" << std::endl;
}

int main(int argc, char *argv[])
{
  auto appConfig = realm::App::configuration();
  appConfig.app_id = argv[1];
  auto app = realm::App(appConfig);
  auto user = app.login(realm::App::credentials::anonymous()).get();
  auto syncConfig = user.flexible_sync_configuration();
  auto syncedRealm = realm::db(syncConfig);

  std::cout << "DB初期化" << std::endl;
  // auto config = realm::db_config();
  // auto realm = realm::db(std::move(config));

  auto initialItem = Todo{
      .summary = "Initial Document.",
      .isComplete = false,
  };

  std::cout << "DB上に初期ドキュメント作成: ";
  printItem(initialItem);

  syncedRealm.write([&]
                    { syncedRealm.add(std::move(initialItem)); });

  auto collection = syncedRealm.objects<Todo>();
  auto imcompletedItems = collection.where(
      [](auto &item)
      { return item.isComplete == false; });
  auto item = imcompletedItems[0];
  std::cout << "DBからドキュメントを探した結果: ";
  printItem(item);

  std::cout << "ドキュメントをアップデートしDBを更新(item.isComplete=true;item.summary=\"Updated Document.\";)" << std::endl;
  syncedRealm.write([&]
                    { item.isComplete = true;
              item.summary = "Updated Document."; });

  auto completedItems = collection.where(
      [](auto &item)
      { return item.isComplete == true; });
  std::cout << "改めてDBからドキュメントを探した結果: ";
  printItem(completedItems[0]);

  syncedRealm.close();
  return 0;
}
