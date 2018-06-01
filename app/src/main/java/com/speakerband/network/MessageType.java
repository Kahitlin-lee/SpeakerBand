package com.speakerband.network;

/**
 * Created by Catalina on 07/09/2017.
 */
public enum MessageType
{
    //Tipo cuando se recibe  texto
    TEXT,
    //Tipo cuando se recibe  una imagen tomada con la camara
    IMAGE,
    //Tipo cuando comienza a enviar la cancion, y recibe el principio de la misma
    SONG_START,
    //Tipo de mientras envia los array de bytes
    SONG,
    //Tipo de cuando ya a terminado de enviar la cancion
    SONG_END,
    //Señal para que re reproduzcan las canciones
    PLAY,

    PREPARE_PLAY,

    PREPARADO,

    SOY_LIDER,

    PAUSAR,

    PASAR_CANCION

}
