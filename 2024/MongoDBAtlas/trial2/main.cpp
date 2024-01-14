#include <iostream>
#include <string>
#include <cpprealm/sdk.hpp>

namespace realm
{
  struct Todo1
  {
    realm::primary_key<realm::object_id> _id{realm::object_id::generate()};
    std::string summary;
    std::string isComplete;
    std::string owner_id;
  };
  // REALM_SCHEMA(Todo, _id, summary, isComplete, owner_id);
  struct Todo
  {
    realm::primary_key<realm::object_id> _id{realm::object_id::generate()};
    std::string summary;
    bool isComplete;
    std::string owner_id;
  };
  REALM_SCHEMA(Todo, _id, summary, isComplete, owner_id);
}

template <typename T>
void printItem(T item)
{
  std::cout << "summary:" << (std::string)item.summary << ", isComplete:" << (bool)item.isComplete << std::endl;
}

int main(int argc, char *argv[])
{
  std::cout << "Atlas C++ SDK Sample" << std::endl;
  auto config = realm::db_config();
  auto realm = realm::db(std::move(config));

  auto todo = realm::Todo{
      .summary = "Create my first todo item",
      .isComplete = false,
  };

  // DB上にドキュメント作成
  realm.write(
      [&]
      {
        realm.add(std::move(realm::Todo{
            .summary = "Initial Document.",
            .isComplete = false,
        }));
      });

  // DBからドキュメントを探す
  auto collection = realm.objects<realm::Todo>();
  auto imcompletedItems = collection.where(
      [](auto &item)
      { return item.isComplete == false; });

  auto item = imcompletedItems[0];
  printItem(item);

  // アップデート
  realm.write([&]
              { 
                item.isComplete = true; 
                item.summary = "Updated Document.";
                });

  // 改めてDBからドキュメントを探す
  auto completedItems = collection.where(
      [](auto &item)
      { return item.isComplete == true; });

  printItem(completedItems[0]);

  realm.close();
  return 0;
}
