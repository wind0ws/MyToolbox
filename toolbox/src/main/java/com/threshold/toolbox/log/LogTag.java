package com.threshold.toolbox.log;

import java.lang.annotation.*;

/**
 * 在无法通过调用栈自动推断 TAG 时，用此注解指定类的 Log TAG。
 * <p>
 * 一般与 {@link SLog}、{@link com.threshold.toolbox.log.llog.LLog} 等配合使用。
 * <p>
 * 匿名内部类、局部类、成员内部类中打 log 时，若该类自身未标注本注解也未定义 TAG 字段，
 * {@link TagFinder} 会沿外部类（enclosing class）和父类链查找，因此只需在外部类或父类上
 * 标注 {@code @LogTag("YourTag")} 或定义 {@code public static final String TAG = "YourTag";} 即可。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface LogTag {
    String value();
}
