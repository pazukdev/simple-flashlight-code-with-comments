package by.pazuk1985.SimpleFlashlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;             // для работы с камерой импортируем эти
import android.hardware.Camera.Parameters;  // два класса
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

/**
 * Created by pazukdev on 01.04.2017.
 */

// создаем стартовую и единственную в приложении Activity MainActivity
public class MainActivity extends Activity {   // наследуемся от Activity

    // сразу объявим несколько переменных, которые нам в дальнейшем пронадобятся:

    private boolean hasFlash;      // это boolean переменная для определения есть ли на
                                   // устройстве камера, а соответственно и вспышка,
                                   // которую мы будем использовать в качестве нашего фонарика
    private Camera camera;         // также объявим переменную camera
    private Parameters parameters; // и переменную parameters. В дальнейшем увидите, зачем они нам.

    // далее напишем методы, нужные для работы нашего фонарика. Весь код функционала фонарика
    // будет содержаться в этих методах. А в теле метода onCreate у нас не будет никакого
    // кода, кроме вызова этих методов. На мой взгляд, это упорядочивает код, а также
    // позволит в дальнейшем проще и удобнее подвергать код модификациям.
    //
    // по задумке, с точки зрения функционала, фонарик не должен обладать никакими
    // дополнительными функциями кроме собственно освещения при помощи вспышки.
    // Включаться должен сразу при открытии приложения (т.е. тут не будет кнопки вкл.-выкл.).
    // При этом фонарик для удобства должен продолжать работать при выключении экрана
    // системной кнопкой Питание (Power) а также сворачивании приложения в фон системной
    // кнопкой Домой (Home).
    // Выключаться фонарик должен при нажатии системной кнопки Назад (Back) в приложении,
    // при котором, собственно, штатно закрывается приложение и уничтожается наша
    // единственная Activity.
    //
    //
    // Итак, мы создали нашу Activity. Теперь давайте перейдем в файл нашего 
    // манифеста AndroidManifest.xml и пропишем там нужные разрешения для приложения для работы с камерой:
    // <uses-permission android:name="android.permission.CAMERA" /> и
    // <uses-feature android:name="android.hardware.camera" />
    // А для того, чтобы приложение работало с такими разрешениями на камеру (они 
	// на данный момент уже устаревшие) на поздних версиях Android (начиная с 6.0)
    // надо открыть файл build.gradle и прописать в targetSdkVersion версию 22.
    // Это позволит приложению получать на поздних версиях Android разрешения
    // так же, как и на старых, при установке.
    // Ну, а те, кто хочет освоить новый подход Google к получению приложениями разрешений 
    // и внедрить его в наш фонарик может начать, например, вот с этой 
	// статьи на Хабре: https://habrahabr.ru/post/278945/
    // Я пока не стал.
    //
    //
    //
    //
    // Вернемся в наш MainActivity.
    // Для начала напишем пару AlertDialog. Они нам будут нужны в случае, если девайс, на
    // котором будет установлено приложение не будет иметь камеры (а соответственно
    // и вспышки). Или, если камера будет по какой либо причине занята. Оба AlertDialog
    // будут в своих методах. И оба будут содержать одну единственную кнопку, при нажатии
    // на которую приложение будет закрываться.
    //
    // первый AlertDialog, который будет показываться в случае, если девайс, на котором
    // будет запускаться приложение не будет иметь камеры:

    private void appStartErrorDialog() {
        final AlertDialog alertDialogAppStartError = // обратите внимание на ключевое слово final. 
                                                     // Для чего мы объявили AlertDialog final - чуть позже 
				new AlertDialog.Builder(MainActivity.this).create();
        alertDialogAppStartError.setTitle(getResources()     // "включаем" Title нашего AlertDialog.
                .getString(R.string.app_start_error_title)); // Тут только ссылка. Сам текст в ресурсах в strings.xml
        alertDialogAppStartError.setMessage(getResources()       // сеттер для части Message (основного
                .getString(R.string.app_start_error_message));   // текста AlertDialog). Текст, опять же, в ресурсах.
        alertDialogAppStartError.setButton(DialogInterface.BUTTON_POSITIVE, // сатавим единственную кнопку "OK"
                getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish(); // пишем действие при нажатии на кнопку - это закрытие Activity, 
                    }             // а вместе с ним и приложения
                });
        alertDialogAppStartError.setCancelable(true); // разрешим пользователю убрать наш AlertDialog 
		                                              // нажатием на кнопку Back
        alertDialogAppStartError.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) { // Но включим слушатель на эту кнопку
                Toast.makeText(MainActivity.this, // создадим Toast, который будет показываться, 
				                                  // при нажатии кнопки Back
                        getResources()
                        .getString(R.string.alertdialog_oncancel_toast_text) // текст нашего Toast опять же
						                                                     // в файле строковых ресурсов
                        , Toast.LENGTH_LONG).show();
                alertDialogAppStartError.show(); // Вызываем показ нашего диалога прямо отсюда. 
            }                                    // Чуть ниже объясню зачем
        });
        alertDialogAppStartError.show(); // вызываем метод sow() для нашего диалога

    }

    // Итак, объясню зачем этот вызов показа диалога из самого себя (из тела слушателя кнопки Back).
    // В setCancelable мы разрешили пользователю закрыть диалоговое окно кнопкой Back. 
	// Что, казалось бы, не логично, т.к. такое закрытие диалогового окна - это
    // плохой вариант развития событий. Т.к. нарушается задуманный алгоритм:
    // пользователь должен нажать кнопку "ОК" в диалоговом окне и этим закрыть приложение.
    // Но мы вовсе не собираемся разрешать пользователю вот так вот просто избавиться от 
	// нашего диалогового окна.
    // Так почему же мы просто не не передали в setCancelable параметр false?
    // Мне хотелось, чтобы кнопка Back не просто не срабатывала, а пользователь при этом 
	// видел подсказку:
    // "Вы не можете выйти из диалогового окна нажатием кнопки Назад (Back). Нажмите ОК в диалоговом окне".
    // Реализовать это я хотел при помощи показа Toast. Однако Toast не показывался при запрете 
	// на Cancelable. При установке запрета при нажатии на кнопку Back
    // просто ничего не происходит. Поэтому мы позволяем диалогу
    // закрыться. Диалог закрывается. Показывается Toast с нашим сообщением. И тут же снова диалог 
    // возникает перед пользователем - для этого мы и вызываем его метод show прямо  
	// из setOnCancelListener. А для того, чтобы такой возов стал возможен
    // мы по требованию Студии в создании объекта alertDialog добавили ключевое 
	// слово final (что это за ключевое слово, и что становится с тем,
    // к чему оно применено, коротко рассказывается, например, вот сдесь:
    // http://developer.alexanderklimov.ru/android/java/class.php
    // Закрытие и открытие снова диалоговога окна происходит очень быстро. Так что  
	// для пользователя окно, по сути, как было так и осталось. Просто теперь при
	// попытке убрать его кнопкой Back, пользователь видит всплывающую подсказку.
    // 
    //
    // Ремарка на счет ссылок на строковый ресурс. Файл strings.xml со строковыми ресурсами 
	// находится в папке values. Тексты для приложения следует располагать там, 
	// а не в коде. В коде - только ссылки на соответствующие строки из strings.xml.
	// В будущем это пригодится. Например, для создания локализаций приложения на других языках.
    //
    // Теперь метод с AlertDialog'ом на случай, если камера по той или иной причине занята. 
    // Все аналогично предыдущему диалогу. 

    private void cameraStartErrorDialog() {
        final AlertDialog alertDialogCameraStartError =
                new AlertDialog.Builder(MainActivity.this).create();
        alertDialogCameraStartError.setTitle(getResources()
                .getString(R.string.camera_start_error_title));
        alertDialogCameraStartError.setMessage(getResources()
                .getString(R.string.camera_statr_error_message));
        alertDialogCameraStartError.setButton(DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        alertDialogCameraStartError.setCancelable(true);
        alertDialogCameraStartError.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Toast.makeText(MainActivity.this, getResources()
                        .getString(R.string.alertdialog_oncancel_toast_text)
                        , Toast.LENGTH_LONG).show();
                alertDialogCameraStartError.show();

            }
        });
        alertDialogCameraStartError.show();

    }

    // Теперь напишем метод для определения есть ли на нашем устройстве собственно камера.
    // Тут воспользуемся нашей заранее объявленной переменной hasFlash:

    private void hasFlashCheck() {
        hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if(!hasFlash) {            // если у нас нет FEATURE_CAMERA_FLASH, т.е. нет вспышки камеры,
            appStartErrorDialog(); // то вызываем метод с нашим первым AlertDialog
        }

        else {
            try {
                camera = Camera.open();   // в противном случае вызываем метод open() для нашей camera
            }                             // (помните, мы переменную camera также объявили заранее в самом начале?)
            catch (RuntimeException e) {  // при этом готовимся в случае необходимости поймать исключение RuntimeException
                e.printStackTrace();      // при проблемах с открытием камеры
                Log.e("Error, can't start: ", e.getMessage()); // и, в случае таковых, заносим соответствующую запись в Log
                cameraStartErrorDialog(); // и в дополнение еще и попробуем показать наш второй AlertDialog
            }
        }

    }

    // напишем метод getCamera

    private void getCamera() {
        try {

            if(camera != null) {                     // если после попытки вызова камеры Camera.open()
                                                     //	она отозвалась,
                parameters = camera.getParameters(); // т.е. не ровна нулю, то пробуем 
            }                                        // получить параметры методом getParameters()

        }
        catch (RuntimeException e) { // при этом опять же отлавливаем возможное Exception
            e.printStackTrace();
            Log.e("Error, can't start: ", e.getMessage());
            cameraStartErrorDialog();
        }
    }

    // теперь напишем метод, который будет включать вспышку в режиме постоянного горения:

    private void flashOn() {
        if (parameters != null) { // если наши параметры parameters успешно получены
            List supportedFlashModes = parameters.getSupportedFlashModes(); // то. И тут у нас опять if.
            if(supportedFlashModes.contains(Parameters.FLASH_MODE_TORCH)) { // Если поддерживаемые режимы вспышки 
			                                                                // содержат параметр FLASH_MODE_TORCH
                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);       // то, собственно, его и выставляем
            }
            else if(supportedFlashModes.contains(Parameters.FLASH_MODE_ON)) { // если поддерживаемые режимы вспышки 
			                                                                  // содержат параметр FLASH_MODE_ON 
                parameters.setFlashMode(Parameters.FLASH_MODE_ON);           // то, опять же, его и выставляем
            }
            else camera = null; // в противном случае оставить камеру в свободном исходном положении
            if(camera != null) { // далее, опять же, если камера отзывается
                camera.setParameters(parameters); // то включаем установленный выше параметр
                camera.startPreview();            // вызываем метод камеры startPreview() собственно включающий камеру
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { // далее еще одна проверка. Если версия 
                                                                              // Android устройства 3.0 или новее
					try {                                                        
                        {
                            camera.setPreviewTexture(new SurfaceTexture(0)); // то включаем в камере такой параметр
                        }
                    } catch (IOException e) { // отлавливаем возможное IOException
                        e.printStackTrace();
                        Log.e("Error, can't start: ", e.getMessage());
                        cameraStartErrorDialog();
                    }
                }

            }
        }
    }
    // несколько пояснений по вышеописанному методу.
    // По поводу первого if'a c FLASH_MODE_TORCH и FLASH_MODE_ON. Вещь увидена в видео
    // "Как создать приложение Фонарик для Android" на канале Start Android. 
	// Автор говорит, что в зависимости от производителя девйса имя режима включения вспышки
    //  может быть как первый так и второй вариант. Собственно, в зависимости от этого,
    // мы и применяем или первый или второй параметр.
    // На счет включения camera.setPreviewTexture(new SurfaceTexture(0)) для 
	// версий Android >= 3.0 - это также было увидено в данном видео.
    // Ссылка на него: https://www.youtube.com/watch?v=uR3eAZP3QNA&vl=ru
    //
    //
    // теперь напишем метод для выключения фонарика:

    private void  flashOff() {
        parameters = camera.getParameters(); // получаем параметры с камеры
        parameters.setFlashMode(Parameters.FLASH_MODE_OFF); // находим параметр переводящий вспышку 
		                                                    // в выключенное состояние
        camera.setParameters(parameters); // и применяем новый параметр
        camera.stopPreview(); // вызываем метод обратный методу startPreview() в который мы вызывали 
		                      // для включения камеры в методе flashOn() 
        camera.release();     // вызываем метод release() освобождающий камеру
        camera = null;        // переводим камеру в исходное дефолтное состояние покоя
    }

    // переопределяем метод onCreate создания нашей Activity:

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);   // все как обычно. Указываем супер-метод
        setContentView(R.layout.layout_main); // подключаем к Activity наш макет экрана layout_main.xml

        // а теперь, как я и предупреждал в начале, у нас весь код состоит из вызова написанных методов:
        hasFlashCheck(); // проверяем девайс на наличие вспышки
        getCamera();     // вызываем метод getCamera()
        flashOn();       // вызываем метод flashOn, включающий вспышку

    }

    // а теперь переопределим метод onDestroy, который вызывается последним при завершении работы приложения
    // в частности, при нажатии системной кнопки Back из приложения:

    @Override
    protected void onDestroy() {
        flashOff();        // вызываем метод выключения вспышки, 
		                   // собственно, ради чего мы и занялись переопределением метода onDestroy()  
        super.onDestroy(); // наш суперметод

    }
}
// вероятно, вписывать метод flashOff() в onDestroy() это не очень правильно. В просмотренных мной 
// статьях и форумах всегда рекомендуют совершать эти действия раньше, в методе onStop().
// Однако в таком случае у меня не реализуется задумка, чтобы фонарик работал при переносе приложения
// в фон (т.е., когда нажимаем кнопку Home и делаем что-то в телефоне).
// Поэтому и вызываю метод flashOff() именно из onDestroy().
//
//
// собственно, вот и все.
//
//
// с уважением,
// pazukdev

