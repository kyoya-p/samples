#include <iostream>
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

int main(int argc,char* argv[])
{
  auto appConfig = realm::App::configuration();
  appConfig.app_id = argv[1];
  auto app = realm::App(appConfig);

  std::cout << "Atlas C++ SDK Sample APPID:" << appConfig.app_id << std::endl;
  auto config = realm::db_config();
  auto realm = realm::db(std::move(config));
  // auto todos = realm.objects<realm::Todo>();
  auto todo = realm::Todo{.summary = "Create my first todo item",
                         };
  realm.write([&]
              { realm.add(std::move(todo)); });
  realm.close();
  return 0;
}
