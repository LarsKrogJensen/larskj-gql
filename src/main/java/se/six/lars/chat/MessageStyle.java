package se.six.lars.chat;


import org.immutables.value.Value;
import se.six.lars.codec.KryoCodecAware;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.CLASS)
@Value.Style (
    typeImmutable = "*Impl",
    passAnnotations = {KryoCodecAware.class}
)
@Value.Immutable
public @interface MessageStyle
{
}
