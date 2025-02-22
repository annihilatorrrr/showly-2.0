package com.michaldrabik.ui_lists.details.views.grid

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.michaldrabik.common.Config
import com.michaldrabik.ui_base.R
import com.michaldrabik.ui_base.utilities.extensions.dimenToPx
import com.michaldrabik.ui_base.utilities.extensions.gone
import com.michaldrabik.ui_base.utilities.extensions.isTablet
import com.michaldrabik.ui_base.utilities.extensions.onClick
import com.michaldrabik.ui_base.utilities.extensions.screenWidth
import com.michaldrabik.ui_base.utilities.extensions.visible
import com.michaldrabik.ui_base.utilities.extensions.visibleIf
import com.michaldrabik.ui_lists.databinding.LayoutListDetailsItemGridBinding
import com.michaldrabik.ui_lists.databinding.ViewListDetailsItemGridTitleBinding
import com.michaldrabik.ui_lists.details.recycler.ListDetailsItem
import com.michaldrabik.ui_lists.details.views.ListDetailsItemView
import com.michaldrabik.ui_model.SortOrder.RATING
import com.michaldrabik.ui_model.SortOrder.USER_RATING
import java.util.Locale
import java.util.Locale.ENGLISH
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class ListDetailsGridTitleItemView : ListDetailsItemView {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewListDetailsItemGridTitleBinding.inflate(LayoutInflater.from(context), this)
  private val contentBinding = LayoutListDetailsItemGridBinding.bind(binding.listDetailsGridItemRoot)

  private val width by lazy {
    val span = if (context.isTablet()) Config.LISTS_GRID_SPAN_TABLET else Config.LISTS_GRID_SPAN
    val itemSpacing = context.dimenToPx(R.dimen.spaceSmall)
    val screenMargin = context.dimenToPx(R.dimen.screenMarginHorizontal)
    val screenWidth = screenWidth().toFloat()
    ((screenWidth - (screenMargin * 2.0)) - ((span - 1) * itemSpacing)) / span
  }
  private val height by lazy { width * 1.7305 }

  init {
    layoutParams = LayoutParams(width.toInt(), height.toInt())

    clipToPadding = false
    clipChildren = false

    binding.listDetailsGridItemRoot.onClick {
      if (item.isEnabled && !item.isManageMode) {
        itemClickListener?.invoke(item)
      }
    }

    imageLoadCompleteListener = { loadTranslation() }
    initSwipeListener()
    initDragListener()
  }

  override val imageView: ImageView = contentBinding.listDetailsGridItemImage
  override val placeholderView: ImageView = contentBinding.listDetailsGridItemPlaceholder

  private fun initSwipeListener() {
    var x = 0F
    binding.listDetailsGridItemRoot.setOnTouchListener { _, event ->
      if (item.isManageMode) {
        return@setOnTouchListener false
      }
      if (event.action == MotionEvent.ACTION_DOWN) x = event.x
      if (event.action == MotionEvent.ACTION_UP) x = 0F
      if (event.action == MotionEvent.ACTION_MOVE && abs(x - event.x) > 50F) {
        itemSwipeStartListener?.invoke()
        return@setOnTouchListener true
      }
      false
    }
  }

  private fun initDragListener() {
    contentBinding.listDetailsGridItemHandle.setOnTouchListener { _, event ->
      if (item.isManageMode && event.action == MotionEvent.ACTION_DOWN) {
        itemDragStartListener?.invoke()
      }
      false
    }
  }

  override fun bind(item: ListDetailsItem) {
    clear()
    this.item = item

    with(contentBinding) {
      listDetailsGridItemProgress.visibleIf(item.isLoading)

      with(binding.listDetailsGridItemTitle) {
        if (item.isShow()) {
          text =
            if (item.translation?.title.isNullOrBlank()) item.requireShow().title
            else item.translation?.title
        }
        if (item.isMovie()) {
          text =
            if (item.translation?.title.isNullOrBlank()) item.requireMovie().title
            else item.translation?.title
        }
      }

      with(listDetailsGridItemImage) {
        if (item.isShow()) setImageResource(R.drawable.ic_television)
        if (item.isMovie()) setImageResource(R.drawable.ic_film)
      }

      with(listDetailsGridItemBadge) {
        val inCollection = item.isWatched || item.isWatchlist
        visibleIf(inCollection)
        if (inCollection) {
          val color = if (item.isWatched) R.color.colorAccent else R.color.colorGrayLight
          imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, color))
        }
      }

      with(listDetailsGridItemRating) {
        when {
          item.isRankDisplayed -> gone()
          item.sortOrder == RATING -> bindRating(item)
          item.sortOrder == USER_RATING && item.userRating != null -> {
            visible()
            text = String.format(ENGLISH, "%d", item.userRating)
          }
          else -> gone()
        }
      }

      listDetailsGridItemRank.visibleIf(item.isRankDisplayed)
      listDetailsGridItemRank.text = String.format(Locale.ENGLISH, "%d", item.rankDisplay)

      listDetailsGridItemHandle.visibleIf(item.isManageMode)
    }
    loadImage(item)
  }

  private fun bindRating(item: ListDetailsItem) {
    with(contentBinding) {
      var rating = String.format(ENGLISH, "%.1f", item.getRating())

      if (item.isShow()) {
        val isMyHidden = item.spoilers.isMyShowsRatingsHidden && item.isWatched
        val isWatchlistHidden = item.spoilers.isWatchlistShowsRatingsHidden && item.isWatchlist
        val isNotCollectedHidden = item.spoilers.isNotCollectedShowsRatingsHidden && (!item.isWatched && !item.isWatchlist)
        if (isMyHidden || isWatchlistHidden || isNotCollectedHidden) {
          listDetailsGridItemRating.tag = rating
          rating = Config.SPOILERS_RATINGS_HIDE_SYMBOL
        }
      }

      if (item.isMovie()) {
        val isMyHidden = item.spoilers.isMyMoviesRatingsHidden && item.isWatched
        val isWatchlistHidden = item.spoilers.isWatchlistMoviesRatingsHidden && item.isWatchlist
        val isNotCollectedHidden = item.spoilers.isNotCollectedMoviesRatingsHidden && (!item.isWatched && !item.isWatchlist)
        if (isMyHidden || isWatchlistHidden || isNotCollectedHidden) {
          listDetailsGridItemRating.tag = rating
          rating = Config.SPOILERS_RATINGS_HIDE_SYMBOL
        }
      }

      if (item.spoilers.isTapToReveal) {
        with(listDetailsGridItemRating) {
          onClick {
            tag?.let { text = it.toString() }
            isClickable = false
          }
        }
      }

      listDetailsGridItemRating.visible()
      listDetailsGridItemRating.text = rating
    }
  }

  private fun loadTranslation() {
    if (item.translation == null) {
      missingTranslationListener?.invoke(item)
    }
  }

  private fun clear() {
    with(contentBinding) {
      listDetailsGridItemPlaceholder.gone()
      Glide.with(this@ListDetailsGridTitleItemView).clear(listDetailsGridItemImage)
    }
  }
}
