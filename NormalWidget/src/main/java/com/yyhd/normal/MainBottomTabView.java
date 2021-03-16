package com.yyhd.normal;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hanli
 * date 2019/3/5.
 * ps: 底部几个button中间是内容的ui组件
 */
public class MainBottomTabView extends RelativeLayout implements View.OnClickListener {

    /**
     * 对应的内容适配器
     */
    private Adapter mAdapter;

    /**
     * 显示内容fragment的容器
     */
    private FrameLayout mFlContentContainer;

    /**
     * 显示底部菜单按钮的容器
     */
    protected LinearLayout mLlBottomContainer;

    /**
     * 按钮菜单的高度
     */
    protected float mMenuItemHeight;


    /**
     * 切换内容fragment的系统管理器
     */
    protected FragmentManager mFragmentManager;

    /**
     * 当前缓存的各个fragment
     */
    protected Map<Integer, Fragment> mFragmentMap;

    /**
     * 缓存的各个view
     */
    protected Map<Integer, View> mItemMenuMap;

    /**
     * 上一个显示的页面位置
     */
    private int mLastPosition = -1;


    public MainBottomTabView(Context context) {
        super(context);
        init(context , null);
    }

    public MainBottomTabView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context , attrs);
    }

    public MainBottomTabView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context , attrs);
    }


    /**
     * 初始化
     * @param context
     */
    private void init(Context context, AttributeSet attrs){
        View.inflate(context , R.layout.view_main_bottom_root , this);

        mFragmentMap = new HashMap<>();
        mItemMenuMap = new HashMap<>();

        mFlContentContainer = findViewById(R.id.fl_container);
        mLlBottomContainer = findViewById(R.id.ll_bottom_container);

        parseParams(context , attrs);
    }

    /**
     * 解析各种初始参数
     * @param context
     * @param attrs
     */
    private void parseParams(Context context , AttributeSet attrs){
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MainBottomTabView);
        mMenuItemHeight = typedArray.getDimension(R.styleable.MainBottomTabView_bottomMenuHeight, 50f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT , (int) mMenuItemHeight);
        mLlBottomContainer.setLayoutParams(params);
    }

    /**
     * 设置对应的内容适配器
     * @param adapter
     */
    public void setAdapter(Adapter adapter , @Nullable FragmentManager fragmentManager){
        this.mAdapter = adapter;
        this.mFragmentManager = fragmentManager;
        if(mFragmentManager == null){
            Context context = getContext();
            if(context instanceof FragmentActivity){
                mFragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
            }
        }
        initContent();
    }

    /**
     * 对内容进行初始化,该方法应该只调用一次
     */
    private void initContent(){
        if(mAdapter != null){
            // 初始化tab图标
            int tabCount = mAdapter.getTabCount();
            for(int i = 0 ; i < tabCount ; i ++){
                FrameLayout flContainer = new FrameLayout(getContext());
                flContainer.setId(i);
                View itemView = getItemMenu(i , flContainer);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0 , ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.weight = 1;
                flContainer.setLayoutParams(layoutParams);
                flContainer.removeAllViews();
                flContainer.addView(itemView);

                flContainer.setOnClickListener(this);
                mLlBottomContainer.addView(flContainer);
            }

            // 初始化第一页的content
            switchFragment(0);
        }
    }

    /**
     * 切换fragment
     * @param position
     */
    public void switchFragment(int position) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        if (position == mLastPosition) {
            // 切换的就是当前fragment，不做任何操作
            return;
        }

        Fragment lastFragment = getFragment(mLastPosition);
        Fragment currentFragment = getFragment(position);

        if (lastFragment != null) {
            // 有上一个fragment，将它隐藏
            transaction.hide(lastFragment);
        }
        if (currentFragment.isAdded()) {
            // 添加过这个fragment，将它显示
            transaction.show(currentFragment);
        } else {
            // 没有添加过，添加它
            Fragment fragmentByTag = mFragmentManager.findFragmentByTag(currentFragment.getTag() + position);
            if (fragmentByTag != null && fragmentByTag.isAdded()) {
                transaction.remove(fragmentByTag);
            }
            transaction.add(R.id.fl_container, currentFragment , currentFragment.getTag() + position);
        }
        transaction.commitAllowingStateLoss();

        // 进行页面切换的回调
        mAdapter.onTabMenuChoosed(position , getItemMenu(position , null));
        if(mLastPosition != -1){
            mAdapter.onTabMenuUnchoosed(mLastPosition , getItemMenu(mLastPosition , null));
        }

        mLastPosition = position;
    }

    /**
     * 获得某一个位置的fragment，会从adapter中获取，和做对应缓存
     * @param position
     * @return
     */
    private Fragment getFragment(int position){
        if(position == -1){
            return null;
        }
        Fragment fragment = mFragmentMap.get(position);
        if(fragment == null){
            fragment = mAdapter.createTabContent(position);
            mFragmentMap.put(position , fragment);
        }
        return fragment;
    }

    /**
     * 获得某一个位置的itemMenu
     * @param position
     * @param container
     * @return
     */
    public View getItemMenu(int position , FrameLayout container){
        View itemMenu = mItemMenuMap.get(position);
        if(itemMenu == null && container != null){
            itemMenu = mAdapter.createTabMenu(position , container);
            mItemMenuMap.put(position , itemMenu);
        }
        return itemMenu;
    }

    @Override
    public void onClick(View v) {
        int position = v.getId();
        if(mAdapter.interceptClick(position)){
            return;
        }

        if(mLastPosition == position){
            // 当前点击的下标就是当前选中的，那么不做处理
            return ;
        }
        switchFragment(position);
    }


    /**
     * 给tab控件使用的数据适配器
     */
    public static abstract class Adapter<T extends View>  {


        /**
         * 获得对应的Tab上的内容fragment
         * @param position
         * @return
         */
        protected abstract Fragment createTabContent(int position);

        /**
         * 获得对应Tab上对应的底部按钮
         * @param position
         * @param menuContainer
         * @return
         */
        protected abstract T createTabMenu(int position, FrameLayout menuContainer);

        /**
         * 当一个tab被选中的时候，菜单需要被改变的状态
         * @param position
         * @param tabMenu
         */
        protected abstract void onTabMenuChoosed(int position , T tabMenu);

        /**
         * 当一个tab被取消选中的时候，菜单需要被改变的状态
         * @param position
         * @param tabMenu
         */
        protected abstract void onTabMenuUnchoosed(int position , T tabMenu);

        /**
         * 返回Tab的总数量
         * @return
         */
        protected abstract int getTabCount();

        /**
         * 是否拦截点击事件
         * @return
         */
        protected abstract boolean interceptClick(int position);

    }
}
