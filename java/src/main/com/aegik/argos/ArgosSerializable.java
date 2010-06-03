package com.aegik.argos;

/**
 * Implemented by classes that wish to be serialized using Argos.
 *
 * @author Christoffer Lerno
 */
public interface ArgosSerializable
{
    Object toSerializableForm();
}
