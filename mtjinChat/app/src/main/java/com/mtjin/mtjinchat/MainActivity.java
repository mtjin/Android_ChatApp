package com.mtjin.mtjinchat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import de.hdodenhof.circleimageview.CircleImageView;

//데이터베이스는 파이어베이스의 리얼타임 데이터베이스를 사용하였다.
public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder> mFirebaseAdapter;

    public static final String MESSAGES_CHILD = "messages"; //DB 하위디렉토리라 생각 (여기다가 메세지들 저장해놀거다)
    private DatabaseReference mFirebaseDatabaseReference; //파이어베이스 데이터베이스를 사용할려면 이 객체를 얻어야한다.
    private EditText mMessageEditText;

    private FirebaseAuth mFirebaseAuth; //인증객체
    private FirebaseUser mFirebaseUser; //인증이 되면 이객체를 얻을 수 있다.
    private GoogleApiClient mGoogleApiClient; //구글인증에필요(여기서는 로그아웃용도로사용)

    private String mUsername; //채팅서비스 사용자 이름과 프로필사진
    private String mPhotoUrl;

    //로그아웃관련할때 실패시 리스너
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        ImageView messageImageView;
        TextView messageTextView;
        CircleImageView photoImageView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.message_tv_name);
            messageImageView = itemView.findViewById(R.id.message_iv_imagemessage);
            messageTextView = itemView.findViewById(R.id.message_tv_message);
            photoImageView = itemView.findViewById(R.id.message_iv_profile);
        }
    }

    private RecyclerView mMessageRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference(); //파이어베이스 realtime db 시작지점을 가리키는 레퍼런스이다. (파이어베이스 리얼타임 데이터베이스 초기화)
        mMessageEditText = findViewById(R.id.main_et_message); //작성한메세지 editText
        mMessageRecyclerView = findViewById(R.id.message_recycler_view); //채팅메세지들 리사이클러뷰

        findViewById(R.id.main_btn_send).setOnClickListener(new View.OnClickListener() { //전송버튼
            @Override
            public void onClick(View v) {
                ChatMessage chatMessage = new ChatMessage(mMessageEditText.getText().toString(),
                        mUsername, mPhotoUrl, null);  //파이어베이스에 저장할 ChatMessage 객체 (채팅아이템)
                mFirebaseDatabaseReference.child(MESSAGES_CHILD) //DB에 (MESSAGES_CHILD)messages라는 이름의 하위디렉토리(?)라는걸 만들고 여기다 데이터를 넣겠다고 생각하면된다.
                        .push() //ChatMessage에서는 id값을 설정을 따로 안했으므로 DB에서 알아서 id를 부여하고 저장해준다. 꺼내올때는 그 부여받은 id로 데이터를 꺼내올 수 있다.
                        .setValue(chatMessage); //DB에 데이터넣음
                mMessageEditText.setText("");
            }
        });

        //로그아웃관련
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this) //기본으로 세팅해줌
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        //파이어베이스 인증
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) { //만약 인증이 안된 유저라면 SignInActivity(로그인화면) 띄워준다.
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else { //인증된 유저라면 해당 유저의 아이디(이름)과 프로필사진을 가져와 변수에 저장한다.
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }

        Query query = mFirebaseDatabaseReference.child(MESSAGES_CHILD); //쿼리문의 수행위치 저장 (파이어베이스 리얼타임데이터베이스의 하위에있는 MESSAGES_CHILD에서 데이터를 가져오겠다는 뜻이다. ==> 메세지를 여기다 저장했으므로)
        FirebaseRecyclerOptions<ChatMessage> options = new FirebaseRecyclerOptions.Builder<ChatMessage>() //어떤데이터를 어디서갖고올거며 어떠한 형태의 데이터클래스 결과를 반환할거냐 옵션을 정의한다.
                .setQuery(query, ChatMessage.class)
                .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder>(options) { //위에서정의한 옵션을 넣어줌줌
            @Override
            protected void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull ChatMessage model) { //홀더에 세팅만해주면됨 (modeld에 데이터가 다들어와있으니 그냥넣어주면됨)
                holder.messageTextView.setText(model.getText());
                holder.nameTextView.setText(model.getName());
                if(model.getPhotoUrl() == null){ //프로필사진없는 경우 기본이미지로 세팅
                    holder.photoImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                            R.drawable.ic_account_circle_black_24dp));   //벡터이미지 넣을떈 이런식으로 넣어줘야한다.
                }else{ //사진이있을 경우(Glide 사용)
                    Glide.with(MainActivity.this).load(model.getPhotoUrl()).into(holder.photoImageView);
                }
            }

            @NonNull
            @Override //뷰홀더가 만들어지는 시점에 호출된다
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.item_message, viewGroup, false); //우리가쓸려는 chatMessage아이템의 뷰객체 생성
                return  new MessageViewHolder(view); //각각의 chatMessage아이템을 위한 뷰를 담고있는 뷰홀더객체를 반환한다.
            }
        };

        // 리사이클러뷰에 레이아웃 매니저와 어댑터를 설정한다.
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false); //레이아웃매니저 생성
        mMessageRecyclerView.setLayoutManager(layoutManager); ////만든 레이아웃매니저 객체를(설정을) 리사이클러 뷰에 설정해줌
        mMessageRecyclerView.setAdapter(mFirebaseAdapter); //어댑터 셋 ( 파이어베이스 어댑터는 액티비티 생명주기에 따라서 상태를 모니터링하게하고 멈추게하고 그런 코드를 작성하도록 되있다.==> 밑에 onStart()와 onStop에 구현해놨다)


        // 새로운 글이 추가되면 제일 하단으로 포지션 이동
        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                LinearLayoutManager layoutManager = (LinearLayoutManager) mMessageRecyclerView.getLayoutManager();
                int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        // 키보드 올라올 때 RecyclerView의 위치를 마지막 포지션으로 이동
        mMessageRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mMessageRecyclerView.smoothScrollToPosition(mFirebaseAdapter.getItemCount());
                        }
                    }, 100);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFirebaseAdapter.startListening();  // FirebaseRecyclerAdapter 실시간 쿼리 시작
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFirebaseAdapter.stopListening(); // FirebaseRecyclerAdapter 실시간 쿼리 중지
    }





    //menu레이아웃만든것을 추가한다.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    //추가한 메뉴에 대한 이벤트처리

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_signout:
                mFirebaseAuth.signOut(); //파이어베이스뿐만아니라
                Auth.GoogleSignInApi.signOut(mGoogleApiClient); //구글로그인도 다 signout해준다.
                mUsername = ""; //유저이름도 초기화를 해준다.
                startActivity(new Intent(this, SignInActivity.class)); //다시 로그인창을 띄워준다.
                finish();
                return true;
            default: //기본값리턴
                return super.onOptionsItemSelected(item);
        }
    }
}
