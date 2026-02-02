#ifndef FIREBASE_SHIM_H
#define FIREBASE_SHIM_H

#ifdef __cplusplus
extern "C" {
#endif

typedef void* FBAppHandle;
typedef void* FBAuthHandle;
typedef void* FBFirestoreHandle;

// Initialize Firebase App. Returns NULL on failure.
FBAppHandle fb_initialize_app(const char* project_id, const char* api_key, const char* app_id);

// Initialize Auth.
FBAuthHandle fb_initialize_auth(FBAppHandle app);

// Initialize Firestore.
FBFirestoreHandle fb_initialize_firestore(FBAppHandle app);

// Sign in with email/password. Returns UID (needs to be freed) or NULL on error.
// Blocks until completion.
char* fb_sign_in(FBAuthHandle auth, const char* email, const char* password);

// Add a document with a "name" and "value" field.
// Blocks until completion.
// Returns 0 on success, -1 on failure.
int fb_add_document(FBFirestoreHandle firestore, const char* collection, const char* doc_name, int value);

// Query documents sorted by field.
// Returns a JSON string (needs to be freed) representing the list of documents or NULL on error.
// Format: [{"name": "...", "value": 123}, ...]
char* fb_query_sorted(FBFirestoreHandle firestore, const char* collection, const char* field);

// Free string returned by shim
void fb_free_string(char* str);

#ifdef __cplusplus
}
#endif

#endif // FIREBASE_SHIM_H
