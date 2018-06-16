package com.speakerband.conexiones.network;

/**
 * Created by Catalina on 07/09/2017.
 */
public enum MessageType
{
    /**
    Tipo para cuando se recibe  texto
     */
    TEXT,
    /**
     Señal Tipo para cuando se recibe  una imagen tomada con la camara
     */
    IMAGE,
    /**
     Señal Tipo cuando comienza a enviar la cancion, y recibe el principio de la misma
     */
    SONG_SEND_START,
    /**
    Señal Tipo de cuando ya a terminado de enviar la cancion
     */
    SONG_SEND_END,
    /**
    Señales para la sync de las  canciones
     */
    PREPARE_PLAY,
    PREPARADO,
    PREPARADO2,
    /**
     * Señal tipo que Indica quien ha aceptado el valo de linder
     */
    SOY_LIDER,

    SOY_LIDER_BOOL_1,
    SOY_LIDER_BOOL_2,
    SOY_LIDER_BOOL_3,
    /**
     *  Señales para poner en play las canciones
     */
    PLAY_CLIENTE,
    PLAY_LIDER,
    /**
     * Señal tipo pausar cancion
     */
    PAUSAR,
    /**
     * Señal tipo para poner en play si esta en puase o en pause si esta en play la cancion
     */
    PLAY_PAUSE
}
