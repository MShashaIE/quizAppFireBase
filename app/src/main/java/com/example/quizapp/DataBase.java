package com.example.quizapp;

import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.quizapp.Model.RankModel;
import com.example.quizapp.Model.CategoryModel;
import com.example.quizapp.Model.ProfileModel;
import com.example.quizapp.Model.QuestionModel;
import com.example.quizapp.Model.TestModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataBase {
    // Access a Cloud Firestore instance from your Activity
    public static FirebaseFirestore db;
    public static List<CategoryModel> g_cat_List = new ArrayList<>();
    public static int cat_index = 0;
    public static int selectedTestIndex = 0;
    public static List<TestModel> g_test_List = new ArrayList<>();
    static int temp;
    public static List<QuestionModel> g_question_list = new ArrayList<>();
    public static List<QuestionModel> g_question_bookmarked = new ArrayList<>();
    public static int usersTotal = 0;
    public static boolean inTopList = false;
    public static List<RankModel> usersList = new ArrayList<>();
    public static List<String> g_bookmarkIdList = new ArrayList<>();
    public static final int NOT_VISITED = 0;
    public static final int UNANSWERED = 1;
    public static final int ANSWERED = 2;
    public static final int REVIEW = 3;
    public static ProfileModel profile = new ProfileModel("n", null, null, 0);
    public static RankModel performance = new RankModel(0, -1, "n");


    static void createUser(String email, String name, MyCompleteListener completeListener) {
        // Create a new user with a first and last name
        Map<String, Object> userData = new HashMap<>();
        userData.put("EMAIL_ID", email);
        userData.put("NAME", name);
        userData.put("TOTAL_SCORE", 0);
        userData.put("BOOKMARKS", 0);

        // multiple writes in single atomic
        WriteBatch batch = db.batch();

        //get user id from firebase authentication table
        DocumentReference userDocReference = db.collection("USERS").document((FirebaseAuth.getInstance().getCurrentUser()).getUid());
        batch.set(userDocReference, userData);

        DocumentReference countDocReference = db.collection("USERS").document("TOTAL_USERS");
        batch.update(countDocReference, "COUNT", FieldValue.increment(1));

        batch.commit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        completeListener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        completeListener.onFailure();
                    }
                });
    }
//    load categories from fire_store Database

    public static void getProfile(MyCompleteListener myCompleteListener) {
//        get user by  firebase user ID
        db.collection("USERS").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        profile.setName(documentSnapshot.getString("NAME"));
                        profile.setEmail(documentSnapshot.getString("EMAIL_ID"));


                        if (documentSnapshot.getString("PHONE") != null) {
                            profile.setPhoneNumber(documentSnapshot.getString("PHONE"));

                        }
                        if (documentSnapshot.get("BOOKMARKS") != null) {
                            profile.setBookmarkCount(documentSnapshot.getLong("BOOKMARKS").intValue());

                        }
                        performance.setName(documentSnapshot.getString("NAME"));
                        performance.setTotalScore(documentSnapshot.getLong("TOTAL_SCORE").intValue());
                        myCompleteListener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        myCompleteListener.onFailure();

                    }
                });
    }

    public static void updateProfileDate(String name, String phone, MyCompleteListener myCompleteListener) {

        Map<String, Object> profileData = new ArrayMap<>();

        profileData.put("NAME", name);
        profileData.put("PHONE", phone);


        db.collection("USERS").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .update(profileData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        profile.setName(name);
                        profile.setPhoneNumber(phone);
                        myCompleteListener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        myCompleteListener.onFailure();
                    }
                });

    }

    public static void loadUserDate(MyCompleteListener myCompleteListener) {
        loadCategories(new MyCompleteListener() {
            @Override
            public void onSuccess() {
                getProfile(new MyCompleteListener() {
                    @Override
                    public void onSuccess() {
                        getTotalUsers(new MyCompleteListener() {
                            @Override
                            public void onSuccess() {
                                loadBookmarkId(myCompleteListener);

                            }

                            @Override
                            public void onFailure() {
                                myCompleteListener.onFailure();
                            }
                        });
                    }

                    @Override
                    public void onFailure() {
                        myCompleteListener.onFailure();
                    }
                });

            }

            @Override
            public void onFailure() {
                myCompleteListener.onFailure();
            }
        });

    }


    public static void loadCategories(MyCompleteListener completeListener) {
        g_cat_List.clear();
        db.collection("QUIZ").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        Map<String, QueryDocumentSnapshot> docList = new ArrayMap<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            docList.put(doc.getId(), doc);
                        }
                        QueryDocumentSnapshot categoryListDoc = docList.get("Categories");


                        long catCount = categoryListDoc.getLong("COUNT");

                        for (int i = 1; i <= catCount; i++) {

                            String catID = categoryListDoc.getString("CAT" + String.valueOf(i) + "_ID");
                            QueryDocumentSnapshot catDoc = docList.get(catID);
                            int NumOfTest = catDoc.getLong("NUM_OF_TESTS").intValue();
                            String catName = catDoc.getString("NAME");
                            g_cat_List.add(new CategoryModel(catID, catName, NumOfTest));
                        }
                        completeListener.onSuccess();
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        completeListener.onFailure();

                    }
                });

    }


    public static void loadTestData(MyCompleteListener myCompleteListener) {
        g_test_List.clear();

//        get document quiz from user selected index
        db.collection("QUIZ").document(g_cat_List.get(cat_index).getDocumentID())
                .collection("TEST_LIST").document("TEST_INFO").get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        int Num_Of_Test = g_cat_List.get(cat_index).getNumOfTests();


                        for (int i = 1; i <= Num_Of_Test; i++) {

                            String test_ID = documentSnapshot.getString("TEST" + String.valueOf(i) + "_ID");
                            int test_time = documentSnapshot.getLong("TEST" + String.valueOf(i) + "_TIME").intValue();
                            g_test_List.add(new TestModel(test_ID, 0, test_time));


                        }

                        myCompleteListener.onSuccess();
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        myCompleteListener.onFailure();
                    }
                });
    }

    public static void loadQuestions(MyCompleteListener myCompleteListener) {
        g_question_list.clear();
        db.collection("Questions")
                .whereEqualTo("CATEGORY", g_cat_List.get(cat_index).getDocumentID())
                .whereEqualTo("TEST", g_test_List.get(selectedTestIndex).getTestID())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                            boolean isBookmarked = false;
                            if (g_bookmarkIdList.contains(documentSnapshot.getId())) {
                                isBookmarked = true;
                            }
                            String question = documentSnapshot.getString("QUESTION");
                            String a = documentSnapshot.getString("A");
                            String b = documentSnapshot.getString("B");
                            String c = documentSnapshot.getString("C");
                            String d = documentSnapshot.getString("D");
                            int answer = documentSnapshot.getLong("ANSWER").intValue();


                            g_question_list.add(new QuestionModel(documentSnapshot.getId(),
                                    question, a, b, c, d, answer, -1, NOT_VISITED, isBookmarked));


                        }
                        Log.d("test", String.valueOf(g_question_list.size()));
                        Log.d("question", String.valueOf(g_question_list));
                        Log.d("test", String.valueOf(g_test_List));
                        Log.d("selectedTestIndex", String.valueOf(selectedTestIndex));
                        Log.d("cat_index", String.valueOf(cat_index));

                        myCompleteListener.onSuccess();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        myCompleteListener.onFailure();
                    }
                });

    }

    public static void sendResult(int score, MyCompleteListener myCompleteListener) {

        WriteBatch writeBatch = db.batch();


//       bookmarks
        Map<String, Object> bookmarkData = new ArrayMap<>();

        for (int i = 0; i < g_bookmarkIdList.size(); i++) {
            bookmarkData.put("BM" + String.valueOf(i) + "_ID", g_bookmarkIdList.get(i));
        }

        DocumentReference bookmarkDocument = db.collection("USERS").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .collection("USER_DATA").document("BOOKMARKS");


        writeBatch.set(bookmarkDocument, bookmarkData);

        DocumentReference userDocument = db.collection("USERS").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));

        writeBatch.update(userDocument, "BOOKMARKS", g_bookmarkIdList.size());
        if (score > g_test_List.get(selectedTestIndex).getTopScore()) {

            DocumentReference scoreDocument = userDocument.collection("USER_DATA").document("MY_SCORE");
            Map<String, Object> testData = new ArrayMap<>();
            testData.put(g_test_List.get(selectedTestIndex).getTestID(), score);
            writeBatch.set(scoreDocument, testData, SetOptions.merge());
        }

        writeBatch.commit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("top", "onSuccess: " + score);
                        g_test_List.get(selectedTestIndex).setTopScore(score);


                        myCompleteListener.onSuccess();


                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        myCompleteListener.onFailure();
                    }
                });

        updateTotalScore();
    }

    public static void loadScore(MyCompleteListener myCompleteListener) {
        db.collection("USERS").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .collection("USER_DATA").document("MY_SCORE")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        for (int i = 0; i < g_test_List.size(); i++) {
                            if (documentSnapshot.get(g_test_List.get(i).getTestID()) != null) {

                                int top = documentSnapshot.getLong(g_test_List.get(i).getTestID()).intValue();
                                g_test_List.get(i).setTopScore(top);

                            }

                        }

                        myCompleteListener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        myCompleteListener.onFailure();
                    }
                });

    }


    public static void updateTotalScore() {

        final int[] sum = {0};
        db.collection("USERS").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .collection("USER_DATA").document("MY_SCORE")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        List<Integer> numbers = new ArrayList<>();
//                        get my_score Data and store it in a map
                        Map<String, Object> map = documentSnapshot.getData();
                        assert map != null;

//                        iterate through the map
                        for (Map.Entry<String, Object> entry : map.entrySet()) {

//                            add numbers to the numbers list
                            numbers.add(Integer.parseInt(entry.getValue().toString()));
                        }

                        for (int i = 0; i < numbers.size(); i++) {
//                            total score summed
                            sum[0] += numbers.get(i);

                        }
                        DocumentReference userDocument = db.collection("USERS").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));
                        userDocument.update("TOTAL_SCORE", sum[0]);
                        performance.setTotalScore(sum[0]);
                        numbers.clear();


                    }
                });

    }

    public static void loadBookmarkId(MyCompleteListener myCompleteListener) {
        g_bookmarkIdList.clear();

        db.collection("USERS").document(FirebaseAuth.getInstance().getUid())
                .collection("USER_DATA").document("BOOKMARKS")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {


                        int count = profile.getBookmarkCount();

                        for (int i = 0; i < count; i++) {
                            String boomMarkId = documentSnapshot.getString("BM" + String.valueOf(i + 1) + "_ID");
                            g_bookmarkIdList.add(boomMarkId);
                        }

                        myCompleteListener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        myCompleteListener.onFailure();
                    }
                });


    }

    public static void loadBookmarkedQues(MyCompleteListener myCompleteListener) {

        g_question_bookmarked.clear();
        temp = 0;

        if (g_bookmarkIdList.size() == 0) {
            myCompleteListener.onSuccess();
        }

//        get question id to fetch info
        for (int i = 0; i < g_bookmarkIdList.size(); i++) {

            String documentID = g_bookmarkIdList.get(i);
            db.collection("Questions").document(documentID)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                g_question_bookmarked.add(new QuestionModel(
                                        documentSnapshot.getId(),
                                        documentSnapshot.getString("QUESTION"),
                                        documentSnapshot.getString("A"),
                                        documentSnapshot.getString("B"),
                                        documentSnapshot.getString("C"),
                                        documentSnapshot.getString("D"),
                                        documentSnapshot.getLong("ANSWER").intValue(),
                                        0,
                                        -1,
                                        false
                                ));
                            }
                            temp++;

                            if (temp == g_bookmarkIdList.size()) {
                                myCompleteListener.onSuccess();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            myCompleteListener.onFailure();
                        }
                    });

        }


    }

    public static void getTopUsers(MyCompleteListener myCompleteListener) {
        usersList.clear();
        String uID = FirebaseAuth.getInstance().getUid();

        db.collection("USERS")
                .whereGreaterThan("TOTAL_SCORE", 0)
                .orderBy("TOTAL_SCORE", Query.Direction.DESCENDING)
                .limit(20)
                .get()

                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        int rank = 1;
                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                            usersList.add(
                                    new RankModel(
                                            queryDocumentSnapshot.getLong("TOTAL_SCORE").intValue(),
                                            rank,
                                            queryDocumentSnapshot.getString("NAME")
                                    )
                            );
                            if (uID.compareTo(queryDocumentSnapshot.getId()) == 0) {
                                inTopList = true;
                                performance.setRank(rank);
                            }
                            rank++;
                        }


                        myCompleteListener.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        myCompleteListener.onFailure();
                    }
                });
    }

    public static void getTotalUsers(MyCompleteListener myCompleteListener) {

        db.collection("USERS").document("TOTAL_USERS")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        usersTotal = documentSnapshot.getLong("COUNT").intValue();


                        myCompleteListener.onSuccess();
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        myCompleteListener.onFailure();
                    }
                });
    }


}