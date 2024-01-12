#include <iostream>
#include <cpprealm/sdk.hpp>

int main(int argc, char *argv[])
{
  auto appConfig = realm::App::configuration();
  appConfig.app_id = argv[1];
  std::cout << "APPID=" << appConfig.app_id << std::endl;

  // Initialize the App, authenticate a user, and open the database
  auto app = realm::App(appConfig);
  auto user = app.login(realm::App::credentials::anonymous()).get();
  auto syncConfig = user.flexible_sync_configuration();
  auto syncedRealm = realm::db(syncConfig);
}

namespace realm
{
  struct Todo
  {
    realm::primary_key<realm::object_id> _id{realm::object_id::generate()};
    std::string name;
    std::string status;
    std::string ownerId;
  };
  REALM_SCHEMA(Todo, _id, name, status, ownerId);
}

int main2()
{
  std::cout << "Atlas CPP Client" << std::endl;

  auto config = realm::db_config();
  auto realm = realm::db(std::move(config));

  auto todo = realm::Todo{.name = "Create my first todo item",
                          .status = "In Progress"};
  realm.write([&]
              { realm.add(std::move(todo)); });

  auto todos = realm.objects<realm::Todo>();

  auto todosInProgress = todos.where(
      [](auto const &todo)
      { return todo.status == "In Progress"; });

  return 0;
}
