/*
 * Copyright 2021 WangYiqian
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */

package com.github.wangyiqian.stockchart.sample.sample2

import android.graphics.Color
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.wangyiqian.stockchart.StockChartConfig
import com.github.wangyiqian.stockchart.childchart.base.HighlightLabelConfig
import com.github.wangyiqian.stockchart.childchart.kchart.KChartConfig
import com.github.wangyiqian.stockchart.childchart.kchart.KChartFactory
import com.github.wangyiqian.stockchart.childchart.kdjchart.KdjChartConfig
import com.github.wangyiqian.stockchart.childchart.kdjchart.KdjChartFactory
import com.github.wangyiqian.stockchart.childchart.macdchart.MacdChartConfig
import com.github.wangyiqian.stockchart.childchart.macdchart.MacdChartFactory
import com.github.wangyiqian.stockchart.childchart.timebar.TimeBarConfig
import com.github.wangyiqian.stockchart.childchart.timebar.TimeBarFactory
import com.github.wangyiqian.stockchart.childchart.volumechart.VolumeChartConfig
import com.github.wangyiqian.stockchart.childchart.volumechart.VolumeChartFactory
import com.github.wangyiqian.stockchart.entities.EmptyKEntity
import com.github.wangyiqian.stockchart.entities.Highlight
import com.github.wangyiqian.stockchart.entities.IKEntity
import com.github.wangyiqian.stockchart.index.Index
import com.github.wangyiqian.stockchart.listener.OnHighlightListener
import com.github.wangyiqian.stockchart.listener.OnLoadMoreListener
import com.github.wangyiqian.stockchart.sample.Data
import com.github.wangyiqian.stockchart.sample.Util
import com.github.wangyiqian.stockchart.sample.R
import com.github.wangyiqian.stockchart.sample.custom.CustomChartConfig
import com.github.wangyiqian.stockchart.sample.custom.CustomChartFactory
import com.github.wangyiqian.stockchart.util.DimensionUtil
import com.github.wangyiqian.stockchart.util.NumberFormatUtil
import kotlinx.android.synthetic.main.activity_sample2.*
import kotlinx.android.synthetic.main.layout_sample2_option_buttons.*

/**
 * @author wangyiqian E-mail: wangyiqian9891@gmail.com
 * @version 创建时间: 2021/3/6
 */
class Sample2Activity : AppCompatActivity() {

    enum class Period {
        DAY,                // 日K
        FIVE_DAYS,          // 5日K
        WEEK,               // 周K
        MONTH,              // 月K
        QUARTER,            // 季K
        YEAR,               // 年K
        FIVE_YEARS,         // 5年K
        YTD,                // 年初至今
        ONE_MINUTE,         // 一分
        FIVE_MINUTES,       // 5分
        SIXTY_MINUTES       // 60分
    }

    private var periodOptionButtons = mutableMapOf<View, Period>()
    private var kChartTypeOptionButtons = mutableMapOf<View, KChartConfig.KChartType>()
    private var indexOptionButton = mutableMapOf<View, Index>()

    private var period = Period.DAY
    private var kChartType: KChartConfig.KChartType = KChartConfig.KChartType.CANDLE()
    private var kChartIndex: Index? = Index.MA()
    private var currentPage = 0

    // 总配置
    private val stockChartConfig = StockChartConfig()

    // K线图工厂与配置
    private var kChartFactory: KChartFactory? = null
    private val kChartConfig = KChartConfig(kChartType = kChartType, index = kChartIndex)

    // 成交量图工厂与配置
    private var volumeChartFactory: VolumeChartFactory? = null
    private val volumeChartConfig = VolumeChartConfig()

    // 时间条图工厂与配置
    private var timeBarFactory: TimeBarFactory? = null
    private val timeBarConfig = TimeBarConfig()

    // macd指标图工厂与配置
    private var macdChartFactory: MacdChartFactory? = null
    private val macdChartConfig = MacdChartConfig()

    // kdj指标图工厂与配置
    private var kdjChartFactory: KdjChartFactory? = null
    private val kdjChartConfig = KdjChartConfig()

    // 自定义示例图与配置
    private var customChartFactory: CustomChartFactory? = null
    private var customChartConfig = CustomChartConfig()

    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample2)

        // StockChart初始化
        initStockChart()

        // 各选项按钮初始化
        initPeriodButtons()
        initKChartTypeButtons()
        initIndexButtons()
        initCustomChartButtons()

        // 切换到到日K，首次加载数据
        changePeriod(Period.DAY)
    }

    /**
     * StockChart初始化
     */
    private fun initStockChart() {

        // 初始化设置各种子图
        initKChart()
        initVolumeChart()
        initTimeBar()
        initMacdChart()
        initKdjChart()
        initCustomChart()

        // 将需要显示的子图的工厂添加进StockChart配置
        stockChartConfig.addChildCharts(
            kChartFactory!!,
            volumeChartFactory!!,
            timeBarFactory!!,
            macdChartFactory!!,
            kdjChartFactory!!,
            customChartFactory!!
        )

        // 最大缩放比例
        stockChartConfig.scaleFactorMax = 2f

        // 最小缩放比例
        stockChartConfig.scaleFactorMin = 0.5f

        // 网格线设置
        stockChartConfig.gridVerticalLineCount = 3
        stockChartConfig.gridHorizontalLineCount = 4

        // 设置滑动到左边界加载更多
        stockChartConfig.addOnLoadMoreListener(object : OnLoadMoreListener {
            override fun onLeftLoadMore() {
                if (!isLoading) {
                    if (period != Period.FIVE_DAYS
                        && period != Period.QUARTER
                        && period != Period.YEAR
                        && period != Period.FIVE_YEARS
                        && period != Period.YTD
                    ) {
                        loadData(page = currentPage + 1, period = period)
                    }
                }
            }

            override fun onRightLoadMore() {}
        })

        // 绑定配置
        stock_chart.setConfig(stockChartConfig)
    }

    /**
     * K线图初始化
     */
    private fun initKChart() {
        kChartFactory = KChartFactory(stock_chart, kChartConfig)

        // 指标线宽度
        kChartConfig.indexStrokeWidth = DimensionUtil.dp2px(this, 0.5f).toFloat()

        // 监听长按信息
        kChartConfig.onHighlightListener = object : OnHighlightListener {
            override fun onHighlightBegin() {}

            override fun onHighlightEnd() {
                tv_highlight_info.text = ""
            }

            override fun onHighlight(highlight: Highlight) {
                val idx = highlight.getIdx()
                val kEntities = stockChartConfig.kEntities
                var showContent = ""

                if (idx in kEntities.indices) {
                    val kEntity = kEntities[idx]
                    if (kEntity is EmptyKEntity) {
                        showContent = ""
                    } else if (kChartConfig.kChartType is KChartConfig.KChartType.LINE
                        || kChartConfig.kChartType is KChartConfig.KChartType.MOUNTAIN
                    ) {
                        val firstIdx = stock_chart.findFirstNotEmptyKEntityIdxInDisplayArea()
                        val price = "最新价:${NumberFormatUtil.formatPrice(kEntity.getClosePrice())}"
                        var changeRatio = "涨跌幅:——"
                        firstIdx?.let {
                            changeRatio = "涨跌幅:${Util.formatChangeRatio(
                                kEntity.getClosePrice(),
                                kEntities[it].getClosePrice()
                            )}"
                        }
                        val volume = "成交量:${Util.formatVolume(kEntity.getVolume())}"

                        showContent = "$price，$changeRatio，$volume"
                    } else {
                        val open = "开盘价:${NumberFormatUtil.formatPrice(kEntity.getOpenPrice())}"
                        val close = "收盘价:${NumberFormatUtil.formatPrice(kEntity.getClosePrice())}"
                        val high = "最高价:${NumberFormatUtil.formatPrice(kEntity.getHighPrice())}"
                        val low = "最低价${NumberFormatUtil.formatPrice(kEntity.getLowPrice())}"
                        val changeRatio =
                            "涨跌幅:${Util.formatChangeRatio(
                                kEntity.getClosePrice(),
                                kEntity.getOpenPrice()
                            )}"
                        val volume = "成交量:${Util.formatVolume(kEntity.getVolume())}"

                        showContent = "$open，$close，$high，$low，$changeRatio，$volume"
                    }

                }

                // 长按信息显示到界面
                tv_highlight_info.text = showContent
            }
        }

        // 图高度
        kChartConfig.height = DimensionUtil.dp2px(this, 250f)

        // 左侧标签设置
        kChartConfig.leftLabelConfig = KChartConfig.LabelConfig(
            5,
            { "${NumberFormatUtil.formatPrice(it)}" },
            DimensionUtil.sp2px(this, 8f).toFloat(),
            Color.parseColor("#E4E4E4"),
            DimensionUtil.dp2px(this, 10f).toFloat(),
            DimensionUtil.dp2px(this, 30f).toFloat(),
            DimensionUtil.dp2px(this, 30f).toFloat()
        )

        // 长按左侧标签配置
        kChartConfig.highlightLabelLeft =
            HighlightLabelConfig(
                textSize = DimensionUtil.sp2px(this, 10f).toFloat(),
                bgColor = Color.parseColor("#A3A3A3"),
                padding = DimensionUtil.dp2px(this, 5f).toFloat()
            )

        // 空心蜡烛边框宽度
        kChartConfig.hollowChartLineStrokeWidth = DimensionUtil.dp2px(this, 1f).toFloat()
    }

    /**
     * 成交量图初始化
     */
    private fun initVolumeChart() {
        volumeChartFactory = VolumeChartFactory(stock_chart, volumeChartConfig)

        // 图高度
        volumeChartConfig.height = DimensionUtil.dp2px(this, 60f)


        // 长按左侧标签配置
        volumeChartConfig.highlightLabelLeft = HighlightLabelConfig(
            textSize = DimensionUtil.sp2px(this, 10f).toFloat(),
            bgColor = Color.parseColor("#A3A3A3"),
            padding = DimensionUtil.dp2px(this, 5f).toFloat(),
            textFormat = { volume ->
                Util.formatVolume(volume = volume.toLong())
            }
        )
    }

    /**
     * 时间条图初始化
     */
    private fun initTimeBar() {
        timeBarFactory = TimeBarFactory(stock_chart, timeBarConfig)

        // 背景色（时间条这里不像显示网格线，加个背景色覆盖掉）
        timeBarConfig.backGroundColor = stockChartConfig.backgroundColor

        // 长按标签背景色
        timeBarConfig.highlightLabelBgColor = Color.parseColor("#A3A3A3")
    }

    /**
     * macd指标图初始化
     */
    private fun initMacdChart() {
        macdChartFactory = MacdChartFactory(stock_chart, macdChartConfig)

        // 图高度
        macdChartConfig.height = DimensionUtil.dp2px(this, 90f)

        // 长按左侧标签配置
        macdChartConfig.highlightLabelLeft = HighlightLabelConfig(
            textSize = DimensionUtil.sp2px(this, 10f).toFloat(),
            bgColor = Color.parseColor("#A3A3A3"),
            padding = DimensionUtil.dp2px(this, 5f).toFloat()
        )
    }

    /**
     * kdj指标图初始化
     */
    private fun initKdjChart() {
        kdjChartFactory = KdjChartFactory(stock_chart, kdjChartConfig)

        // 图高度
        kdjChartConfig.height = DimensionUtil.dp2px(this, 90f)

        // 长按左侧标签配置
        kdjChartConfig.highlightLabelLeft = HighlightLabelConfig(
            textSize = DimensionUtil.sp2px(this, 10f).toFloat(),
            bgColor = Color.parseColor("#A3A3A3"),
            padding = DimensionUtil.dp2px(this, 5f).toFloat()
        )
    }

    /**
     * 自定义示例图初始化
     */
    private fun initCustomChart() {
        customChartFactory = CustomChartFactory(stock_chart, customChartConfig)
        customChartConfig.height = DimensionUtil.dp2px(this, 70f)
        customChartConfig.bigLabel = "这是自定义子图示例"
    }

    // 加载模拟数据
    private fun loadData(page: Int = 0, period: Period) {
        isLoading = true

        fun doAfterLoad(
            kEntities: List<IKEntity>,
            initialPageSize: Int?,
            timeBarType: TimeBarConfig.Type
        ) {
            if (kEntities.isNotEmpty()) {
                // 设置数据
                if (page == 0) {
                    if (initialPageSize != null) {
                        stockChartConfig.setKEntities(
                            kEntities,
                            kEntities.size - initialPageSize,
                            kEntities.size - 1
                        )
                    } else {
                        stockChartConfig.setKEntities(kEntities)
                    }

                } else {
                    stockChartConfig.appendLeftKEntities(kEntities)
                }

                // 设置时间条样式
                timeBarConfig.type = timeBarType

                // 通知更新
                stock_chart.notifyChanged()
                currentPage = page
            } else {
                Toast.makeText(this, "没有更多数据了！", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }

        when (period) {
            Period.DAY -> {
                Data.loadDayData(this, page) { list ->
                    doAfterLoad(list, 60, TimeBarConfig.Type.Day())
                }
            }
            Period.FIVE_DAYS -> {
                Data.loadFiveDayData(this) { list ->
                    doAfterLoad(list, null, TimeBarConfig.Type.FiveDays())
                }
            }
            Period.WEEK -> {
                Data.loadWeekData(this, page) { list ->
                    doAfterLoad(list, 60, TimeBarConfig.Type.Week())
                }
            }
            Period.MONTH -> {
                Data.loadMonthData(this, page) { list ->
                    doAfterLoad(list, 60, TimeBarConfig.Type.Month())
                }
            }
            Period.QUARTER -> {
                Data.loadQuarterData(this) { list ->
                    doAfterLoad(list, null, TimeBarConfig.Type.Quarter())
                }
            }
            Period.YEAR -> {
                Data.loadYearData(this) { list ->
                    doAfterLoad(list, null, TimeBarConfig.Type.Year())
                }
            }
            Period.FIVE_YEARS -> {
                Data.loadFiveYearData(this) { list ->
                    doAfterLoad(list, null, TimeBarConfig.Type.FiveYears())
                }
            }
            Period.YTD -> {
                Data.loadYTDData(this) { list ->
                    doAfterLoad(list, null, TimeBarConfig.Type.YTD())
                }
            }
            Period.ONE_MINUTE -> {
                Data.loadOneMinuteData(this, page) { list ->
                    doAfterLoad(list, 60, TimeBarConfig.Type.OneMinute())
                }
            }
            Period.FIVE_MINUTES -> {
                Data.loadFiveMinutesData(this, page) { list ->
                    doAfterLoad(list, 60, TimeBarConfig.Type.FiveMinutes())
                }
            }
            Period.SIXTY_MINUTES -> {
                Data.loadSixtyMinutesData(this, page) { list ->
                    doAfterLoad(list, 60, TimeBarConfig.Type.SixtyMinutes())
                }
            }
        }
    }

    private fun changePeriod(period: Period) {
        when (period) {
            Period.FIVE_DAYS -> {
                stockChartConfig.scaleAble = false
                stockChartConfig.scrollAble = false
                stockChartConfig.overScrollAble = false
                kChartConfig.index = null
                kChartConfig.kChartType = KChartConfig.KChartType.LINE()
            }
            Period.YEAR, Period.QUARTER, Period.FIVE_YEARS -> {
                stockChartConfig.scaleAble = true
                stockChartConfig.scrollAble = true
                stockChartConfig.overScrollAble = false
                kChartConfig.index = kChartIndex
                kChartConfig.kChartType = kChartType
            }
            Period.YTD -> {
                stockChartConfig.scaleAble = false
                stockChartConfig.scrollAble = false
                stockChartConfig.overScrollAble = false
                kChartConfig.index = kChartIndex
                kChartConfig.kChartType = kChartType
            }
            else -> {
                stockChartConfig.scaleAble = true
                stockChartConfig.scrollAble = true
                stockChartConfig.overScrollAble = true
                kChartConfig.index = kChartIndex
                kChartConfig.kChartType = kChartType
            }
        }
        this.period = period
        loadData(period = this.period)
        refreshOptionButtonsState()
    }

    private fun changeKChartType(kChartType: KChartConfig.KChartType) {

        if (period == Period.FIVE_DAYS && kChartType !is KChartConfig.KChartType.LINE) {
            // 这个period只支持折线图
            return
        }

        this.kChartType = kChartType
        kChartConfig.kChartType = this.kChartType
        stock_chart.notifyChanged()
        refreshOptionButtonsState()
    }

    private fun initPeriodButtons() {
        periodOptionButtons.putAll(
            arrayOf(
                Pair(period_day, Period.DAY),
                Pair(period_five_days, Period.FIVE_DAYS),
                Pair(period_week, Period.WEEK),
                Pair(period_month, Period.MONTH),
                Pair(period_quarter, Period.QUARTER),
                Pair(period_year, Period.YEAR),
                Pair(period_five_years, Period.FIVE_YEARS),
                Pair(period_ytd, Period.YTD),
                Pair(period_one_minute, Period.ONE_MINUTE),
                Pair(period_five_minutes, Period.FIVE_MINUTES),
                Pair(period_sixty_minutes, Period.SIXTY_MINUTES)
            )
        )

        periodOptionButtons.forEach { (button, period) ->
            button.setOnClickListener { changePeriod(period) }
        }
    }

    private fun initKChartTypeButtons() {
        kChartTypeOptionButtons.putAll(
            listOf(
                Pair(kchart_type_candle, KChartConfig.KChartType.CANDLE()),
                Pair(kchart_type_hollow, KChartConfig.KChartType.HOLLOW()),
                Pair(kchart_type_line, KChartConfig.KChartType.LINE()),
                Pair(kchart_type_mountain, KChartConfig.KChartType.MOUNTAIN()),
                Pair(kchart_type_bar, KChartConfig.KChartType.BAR())
            )
        )
        kChartTypeOptionButtons.forEach { (button, kChatType) ->
            button.setOnClickListener { changeKChartType(kChatType) }
        }
    }

    private fun initIndexButtons() {
        indexOptionButton.putAll(
            listOf(
                Pair(index_ma, Index.MA()),
                Pair(index_ema, Index.EMA()),
                Pair(index_boll, Index.BOLL()),
                Pair(index_macd, Index.MACD()),
                Pair(index_kdj, Index.KDJ())
            )
        )

        indexOptionButton.forEach { (button, index) ->
            button.setOnClickListener {
                when (index::class) {
                    Index.MA::class, Index.EMA::class, Index.BOLL::class -> { // 这三个是K线图中的指标
                        kChartIndex =
                            if (kChartIndex != null && kChartIndex!!::class == index::class) {
                                null
                            } else {
                                index
                            }
                        kChartConfig.index = kChartIndex
                    }
                    Index.MACD::class -> {
                        if (stockChartConfig.childChartFactories.contains(macdChartFactory!!)) {
                            stockChartConfig.removeChildCharts(macdChartFactory!!)
                        } else {
                            stockChartConfig.addChildCharts(macdChartFactory!!)
                        }
                    }
                    Index.KDJ::class -> {
                        if (stockChartConfig.childChartFactories.contains(kdjChartFactory!!)) {
                            stockChartConfig.removeChildCharts(kdjChartFactory!!)
                        } else {
                            stockChartConfig.addChildCharts(kdjChartFactory!!)
                        }
                    }
                }
                stock_chart.notifyChanged()
                refreshOptionButtonsState()
            }
        }
    }

    private fun initCustomChartButtons() {
        custom.setOnClickListener {
            if (stockChartConfig.childChartFactories.contains(customChartFactory!!)) {
                stockChartConfig.removeChildCharts(customChartFactory!!)
            } else {
                stockChartConfig.addChildCharts(customChartFactory!!)
            }
            stock_chart.notifyChanged()
            refreshOptionButtonsState()
        }
    }

    /**
     * 选项按钮状态刷新
     */
    private fun refreshOptionButtonsState() {
        periodOptionButtons.forEach { (button, period) ->
            button.isSelected = period == this.period
        }
        kChartTypeOptionButtons.forEach { (button, kChartType) ->
            button.isSelected = kChartType::class == kChartConfig.kChartType::class
        }
        indexOptionButton.forEach { (button, index) ->
            when (index::class) {
                Index.MA::class, Index.EMA::class, Index.BOLL::class -> { // 这三个是K线图中的指标
                    button.isSelected =
                        kChartConfig.index != null && kChartConfig.index!!::class == index::class
                }
                Index.MACD::class -> {
                    button.isSelected =
                        stockChartConfig.childChartFactories.contains(macdChartFactory!!)
                }
                Index.KDJ::class -> {
                    button.isSelected =
                        stockChartConfig.childChartFactories.contains(kdjChartFactory!!)
                }
            }
        }

        custom.isSelected = stockChartConfig.childChartFactories.contains(customChartFactory!!)
    }
}