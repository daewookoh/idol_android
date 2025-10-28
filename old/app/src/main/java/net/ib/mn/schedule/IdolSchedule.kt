package net.ib.mn.schedule

import net.ib.mn.model.ScheduleModel
import java.util.Collections

class IdolSchedule private constructor() {
    @JvmField
    var schedules = ArrayList<ScheduleModel>()
    @JvmField
    var lastSchedule: ScheduleModel? = null
    @JvmField
    var scheduleLocaleString = ""

    private object IdolScheduleHolder {
        val instance = IdolSchedule()
    }

    fun setScheduleItem(sch: ScheduleModel) {
        var i = 0
        lastSchedule = sch
        for (item in schedules) {
            if (item.id == sch.id) break
            i++
        }
        //푸시 타고 들어와서 댓글 달았을때
        //IndexOutOfBoundsException  나오는 경우 있어서  적용
        try {
            schedules[i] = sch
        } catch (e: IndexOutOfBoundsException) {
        }
    }

    fun addScheduleItem(sch: ScheduleModel) {
        schedules.add(sch)
        Collections.sort(schedules) { lhs: ScheduleModel, rhs: ScheduleModel ->
            if (lhs.dtstart === rhs.dtstart) {
                return@sort rhs.allday - lhs.allday
            } else {
                return@sort lhs.dtstart.compareTo(rhs.dtstart)
            }
        }
    }

    fun deleteScheduleItem(id: Int) {
        var i = 0
        for (item in schedules) {
            if (item.id == id) break
            i++
        }
        try {
            schedules.removeAt(i)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    fun clearSchedule() {
        schedules.clear()
    }

    fun sort(sch: ArrayList<ScheduleModel>): ArrayList<ScheduleModel> {
        val sort = ArrayList<ScheduleModel>()
        for (i in 1..31) {
            val tmp = ArrayList<ScheduleModel>()
            for (item in sch) {
                if (item.allday == 1 && item.dtstart.date == i) tmp.add(item)
            }
            Collections.sort(tmp) { lhs: ScheduleModel, rhs: ScheduleModel ->
                lhs.created_at.compareTo(
                    rhs.created_at
                )
            }
            sort.addAll(tmp)
            tmp.clear()
            for (item in sch) {
                if (item.allday == 0 && item.dtstart.date == i) tmp.add(item)
            }
            Collections.sort(tmp) { lhs: ScheduleModel, rhs: ScheduleModel ->
                if (lhs.dtstart === rhs.dtstart) {
                    return@sort lhs.created_at.compareTo(rhs.created_at)
                } else {
                    return@sort lhs.dtstart.compareTo(rhs.dtstart)
                }
            }
            sort.addAll(tmp)
            tmp.clear()
        }
        return sort
    }

    companion object {
        @JvmStatic
        fun getInstance(): IdolSchedule {
            return IdolScheduleHolder.instance
        }
    }
}
