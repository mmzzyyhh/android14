package com.test.xlfresh;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
//import android.widget.ProgressBar;
import android.widget.TextView;

public class MyListView extends ListView implements OnScrollListener 
{
	private static final String TAG = "am10";
	private final static int RELEASE_To_REFRESH = 0;//�ɿ�ˢ��
	private final static int PULL_To_REFRESH = 1;   //����ˢ��
	private final static int REFRESHING = 2;    //����ˢ��
	private final static int DONE = 3; //����ˢ�����
	private final static int LOADING = 4;

	// ʵ�ʵ�padding�ľ����������ƫ�ƾ���ı���
	private final static int RATIO = 3;
	
	private LayoutInflater inflater;
	private LinearLayout headView;
	private TextView tipsTextview;
	private TextView lastUpdatedTextView;
	private ImageView arrowImageView;
	//private ProgressBar progressBar;

	private RotateAnimation animation;//���Ƽ�ͷ��ת������ʵ�ֶ�����ת���̳�Animation��
	private RotateAnimation reverseAnimation;

	// ���ڱ�֤startY��ֵ��һ��������touch�¼���ֻ����¼һ��
	private boolean isRecored;
	
	private int startY;//��¼����ʱ��Y����
	private int firstItemIndex;//�ж��Ƿ��ǵ�һ��
	
	private int headContentWidth;//headview�Ŀ�͸�
	private int headContentHeight;

	private int state;//���ʵ�ǰҳ��״̬

	private boolean isBack;//�Ƿ�ص�

	private OnRefreshListener refreshListener;//ˢ�¼���

	private boolean isRefreshable;//�Ƿ�ˢ��

	public MyListView(Context context) 
	{
		super(context);
		init(context);
	}

	public MyListView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		init(context);
	}

	private void init(Context context) 
	{
		//setCacheColorHint(context.getResources().getColor(R.color.transparent));
		inflater = LayoutInflater.from(context);

		headView = (LinearLayout) inflater.inflate(R.layout.head, null);

		arrowImageView = (ImageView) headView.findViewById(R.id.head_arrowImageView);
		arrowImageView.setMinimumWidth(50);
		arrowImageView.setMinimumHeight(20);
		
		//progressBar = (ProgressBar) headView.findViewById(R.id.head_progressBar);		
		tipsTextview = (TextView) headView.findViewById(R.id.head_tipsTextView);
		lastUpdatedTextView = (TextView) headView.findViewById(R.id.head_lastUpdatedTextView);

		// ����headView�Ŀ�͸�
		measureView(headView);
		headContentHeight = headView.getMeasuredHeight();
		headContentWidth = headView.getMeasuredWidth();
		
		headView.setPadding(0, -1 * headContentHeight, 0, 0);
		headView.invalidate();

		Log.v(TAG, "width:" + headContentWidth + " height:" + headContentHeight);

		addHeaderView(headView, null, false);
		setOnScrollListener(this);

		animation = new RotateAnimation(0, -180,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		animation.setInterpolator(new LinearInterpolator());
		animation.setDuration(250);
		animation.setFillAfter(true);

		reverseAnimation = new RotateAnimation(-180, 0,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f,
				RotateAnimation.RELATIVE_TO_SELF, 0.5f);
		reverseAnimation.setInterpolator(new LinearInterpolator());
		reverseAnimation.setDuration(200);
		reverseAnimation.setFillAfter(true);

		state = DONE;
		isRefreshable = false;
	}

	public void onScroll(AbsListView arg0, int firstVisiableItem, int arg2,	int arg3) 
	{
		firstItemIndex = firstVisiableItem;
		//Log.v(TAG, "firstItemIndex: "+firstItemIndex);
	}

	public void onScrollStateChanged(AbsListView arg0, int arg1) 
	{
	}

	public boolean onTouchEvent(MotionEvent event) 
	{
		// Log.v(TAG, "isRefreshable: "+isRefreshable);
		if (isRefreshable) 
		{
			switch (event.getAction()) 
			{
			case MotionEvent.ACTION_DOWN:
				if (firstItemIndex == 0 && !isRecored) 
				{
					isRecored = true;
					
					// ������Ļ��λ��
					startY = (int) event.getY();	
					Log.v(TAG, "��downʱ���¼��ǰλ��" + " startY:"+startY);
				}
				break;

			case MotionEvent.ACTION_UP:
				if (state != REFRESHING && state != LOADING) 
				{
					if (state == DONE) 
					{
						// ʲô������
					}
					
					if (state == PULL_To_REFRESH) 
					{
						state = DONE;
						changeHeaderViewByState();
						Log.v(TAG, "������ˢ��״̬����done״̬");
					}
					
					if (state == RELEASE_To_REFRESH) 
					{
						state = REFRESHING;
						changeHeaderViewByState();
						onRefresh();
						Log.v(TAG, "���ɿ�ˢ��״̬����done״̬");
					}
				}

				isRecored = false;
				isBack = false;
				break;

			case MotionEvent.ACTION_MOVE:
				int tempY = (int) event.getY();
				//Log.v(TAG, "tempY: " + tempY);
				
				/** 							 
				 * ��ָ�ƶ�������tempY���ݻ᲻�ϱ仯,��������firstItemIndex,�����ﶥ��,
				 * ��Ҫ��¼��ָ������Ļ��λ��: startY = tempY ,������λ�ñȽ�ʹ��
				 * 
				 * �����ָ����������,tempY�����仯,��tempY-startY>0,������Ҫ��ʾheader����
				 * 
				 * ��ʱ��Ҫ����״̬��state = PULL_To_REFRESH
				 */
				if (!isRecored && firstItemIndex == 0) 
				{
					isRecored = true;
					startY = tempY;
					Log.v(TAG, "��moveʱ���¼��λ��" + " startY:"+startY);
				}

				if (state != REFRESHING && isRecored && state != LOADING) 
				{
					/**
					 * ��֤������padding�Ĺ����У���ǰ��λ��һֱ����head��
					 * ����������б�����Ļ�Ļ����������Ƶ�ʱ���б��ͬʱ���й���
					 */					

					// ��������ȥˢ����
					if (state == RELEASE_To_REFRESH) 
					{
						setSelection(0);

						// �������ˣ��Ƶ�����Ļ�㹻�ڸ�head�ĳ̶ȣ����ǻ�û���Ƶ�ȫ���ڸǵĵز�
						if (((tempY - startY) / RATIO < headContentHeight) && (tempY - startY) > 0) 
						{
							state = PULL_To_REFRESH;
							changeHeaderViewByState();
							Log.v(TAG, "���ɿ�ˢ��״̬ת�䵽����ˢ��״̬");
						}
						
						// һ�����Ƶ�����,û����ʾheader����ʱ,Ӧ�ûָ�DONE״̬,������ʺ�С
						else if (tempY - startY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "---���ɿ�ˢ��״̬ת�䵽done״̬");
						}						
						else 
						{
							// �������ˣ����߻�û�����Ƶ���Ļ�����ڸ�head�ĵز�
							// ���ý����ر�Ĳ�����ֻ�ø���paddingTop��ֵ������
						}
					}
					
					// ��û�е�����ʾ�ɿ�ˢ�µ�ʱ��,DONE������PULL_To_REFRESH״̬
					if (state == PULL_To_REFRESH) 
					{
						setSelection(0);

						/**
						 * ���������Խ���RELEASE_TO_REFRESH��״̬
						 * 
						 * ����headContentHeightʱ,����������ȫ��ʾheader����
						 * ����headContentHeightʱ,���ǳ���header���ָ���
						 * 
						 * ��header�����ܹ���ȫ��ʾ���߳�����ʾ,
						 * ��Ҫ����״̬: state = RELEASE_To_REFRESH
						 */
						if ((tempY - startY) / RATIO >= headContentHeight) 
						{
							state = RELEASE_To_REFRESH;
							isBack = true;
							changeHeaderViewByState();
							Log.v(TAG, "��done��������ˢ��״̬ת�䵽�ɿ�ˢ��");
						}
						
						// ���Ƶ�����,û����ʾheader����ʱ,Ӧ�ûָ�DONE״̬
						else if (tempY - startY <= 0) 
						{
							state = DONE;
							changeHeaderViewByState();
							Log.v(TAG, "��done��������ˢ��״̬ת�䵽done״̬");
						}
					}

					// done״̬��
					if (state == DONE) 
					{
						if (tempY - startY > 0) 
						{
							/** 							 
							 * ��ָ�ƶ�������tempY���ݻ᲻�ϱ仯,��������firstItemIndex,�����ﶥ��,
							 * ��Ҫ��¼��ָ������Ļ��λ��: startY = tempY ,������λ�ñȽ�ʹ��
							 * 
							 * �����ָ����������,tempY�����仯,��tempY-startY>0,������Ҫ��ʾheader����
							 * 
							 * ��ʱ��Ҫ����״̬��state = PULL_To_REFRESH
							 */
							//Log.v(TAG, "----------------PULL_To_REFRESH " + (tempY - startY));							
							state = PULL_To_REFRESH;
							changeHeaderViewByState();
						}
					}

					// ����headView��paddingTop
					if (state == PULL_To_REFRESH) 
					{
						//Log.v(TAG, "----------------PULL_To_REFRESH2 " + (tempY - startY));
						headView.setPadding(0, -1 * headContentHeight + (tempY - startY) / RATIO, 0, 0);
					}

					// ��������headView��paddingTop
					if (state == RELEASE_To_REFRESH) 
					{
						headView.setPadding(0, (tempY - startY) / RATIO	- headContentHeight, 0, 0);
					}
				}
				break;
			}
		}
		return super.onTouchEvent(event);
	}

	// ��״̬�ı�ʱ�򣬵��ø÷������Ը��½���
	private void changeHeaderViewByState() 
	{
		switch (state) 
		{
			case RELEASE_To_REFRESH:
				arrowImageView.setVisibility(View.VISIBLE);
				//progressBar.setVisibility(View.GONE);
				tipsTextview.setVisibility(View.VISIBLE);
				lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				arrowImageView.clearAnimation();
				arrowImageView.startAnimation(animation);
	
				tipsTextview.setText("�ɿ�ˢ��");
	
				Log.v(TAG, "��ǰ״̬���ɿ�ˢ��");
				break;
				
			case PULL_To_REFRESH:
				//progressBar.setVisibility(View.GONE);
				tipsTextview.setVisibility(View.VISIBLE);
				lastUpdatedTextView.setVisibility(View.VISIBLE);
				arrowImageView.clearAnimation();
				arrowImageView.setVisibility(View.VISIBLE);
				
				/**
				 *  �Ƿ����»��أ�����RELEASE_To_REFRESH״̬ת������
				 */
				if (isBack) 
				{					
					isBack = false;
					arrowImageView.clearAnimation();
					arrowImageView.startAnimation(reverseAnimation);	
					tipsTextview.setText("����ˢ��");
					//Log.v(TAG, "isBack: " + isBack);
				} 
				else 
				{					
					tipsTextview.setText("����ˢ��");
					//Log.v(TAG, "isBack: " + isBack);
				}
				
				Log.v(TAG, "��ǰ״̬������ˢ��");
				break;
	
			case REFRESHING:
				Log.v(TAG, "REFRESHING...");
				headView.setPadding(0, 0, 0, 0);
	
				//progressBar.setVisibility(View.VISIBLE);
				arrowImageView.clearAnimation();
				arrowImageView.setVisibility(View.GONE);
				tipsTextview.setText("����ˢ��...");
				lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				Log.v(TAG, "��ǰ״̬,����ˢ��...");
				break;
				
			case DONE:
				headView.setPadding(0, -1 * headContentHeight, 0, 0);
	
				//progressBar.setVisibility(View.GONE);
				arrowImageView.clearAnimation();
				arrowImageView.setImageResource(R.drawable.apk);
				tipsTextview.setText("����ˢ��");
				lastUpdatedTextView.setVisibility(View.VISIBLE);
	
				Log.v(TAG, "��ǰ״̬��done");
				break;
		}
	}

	public void setOnRefreshListener(OnRefreshListener refreshListener) 
	{
		this.refreshListener = refreshListener;
		isRefreshable = true;
	}

	public interface OnRefreshListener 
	{
		public void onRefresh();
	}

	public void onRefreshComplete() 
	{
		state = DONE;
		SimpleDateFormat format = new SimpleDateFormat("yyyy��MM��dd��  HH:mm");
		String date = format.format(new Date());
		lastUpdatedTextView.setText("�������:" + date);
		changeHeaderViewByState();
	}

	private void onRefresh() 
	{
		if (refreshListener != null) 
		{
			refreshListener.onRefresh();
		}
	}

	// �˷���ֱ���հ��������ϵ�һ������ˢ�µ�demo���˴��ǡ����ơ�headView��width�Լ�height
	private void measureView(View child) 
	{
		ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) 
        {
        	//Log.v(TAG, "LayoutParams is null.");
            p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        
        if (lpHeight > 0) 
        {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } 
        else 
        {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }

        child.measure(childWidthSpec, childHeightSpec);
	}

	public void setAdapter(BaseAdapter adapter) 
	{
		SimpleDateFormat format=new SimpleDateFormat("yyyy��MM��dd��  HH:mm");
		String date=format.format(new Date());
		lastUpdatedTextView.setText("�������:" + date);
		super.setAdapter(adapter);
	}
}