import androidx.compose.ui.window.ComposeUIViewController
import data.createSampleRepository
import ui.App

fun MainViewController() = ComposeUIViewController {
    App(repository = createSampleRepository())
}
